#!/bin/bash
# ============================================
# 海洋生物知识库 — 本地开发启动脚本
# 分别启动后端 (Spring Boot) 和前端 (Vue 3)
# 适用于 Windows (Git Bash) / Linux / macOS
# ============================================

set -e

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/ocean-server"
FRONTEND_DIR="$PROJECT_DIR/ocean-web"
BACKEND_LOG="$BACKEND_DIR/ocean-server.log"
BACKEND_PID_FILE="$PROJECT_DIR/.backend.pid"
FRONTEND_PID_FILE="$PROJECT_DIR/.frontend.pid"
ES_PID_FILE="$PROJECT_DIR/.es.pid"
ES_HOME="/d/elasticsearch-9.4.2"

# ====== 工具函数 ======

usage() {
    echo "用法: $0 {start|stop|restart|status} [backend|frontend]"
    echo ""
    echo "  省略模块名 = 同时操作前后端"
    echo ""
    echo "  示例:"
    echo "    $0 start          启动前后端"
    echo "    $0 start backend  仅启动后端"
    echo "    $0 stop           停止全部"
    echo "    $0 status         查看状态"
    exit 1
}

check_deps() {
    if [ "$1" = "backend" ] || [ "$1" = "all" ]; then
        if ! command -v java &>/dev/null; then
            echo -e "${RED}[ERROR] 未找到 Java，请安装 JDK 1.8+${NC}"
            exit 1
        fi
        if ! command -v mvn &>/dev/null; then
            echo -e "${RED}[ERROR] 未找到 Maven，请安装 Maven 3.6+${NC}"
            exit 1
        fi
    fi
    if [ "$1" = "frontend" ] || [ "$1" = "all" ]; then
        if ! command -v node &>/dev/null; then
            echo -e "${RED}[ERROR] 未找到 Node.js，请安装 Node.js 16+${NC}"
            exit 1
        fi
    fi
}

check_infra() {
    local ok=true
    # 检查 MySQL
    if command -v mysql &>/dev/null; then
        if mysql -u root -p"${MYSQL_PASSWORD:-smj041209}" -e "SELECT 1" &>/dev/null; then
            echo -e "  ${GREEN}✅ MySQL       运行中${NC}"
        else
            echo -e "  ${YELLOW}⚠️  MySQL      未连接 (检查 MYSQL_PASSWORD 环境变量)${NC}"
            ok=false
        fi
    fi
    # 检查 Redis
    if command -v redis-cli &>/dev/null; then
        if redis-cli ping &>/dev/null; then
            echo -e "  ${GREEN}✅ Redis       运行中${NC}"
        else
            echo -e "  ${YELLOW}⚠️  Redis       未启动${NC}"
            ok=false
        fi
    fi
    # 检查 ES
    if curl -s http://localhost:9200 &>/dev/null; then
        echo -e "  ${GREEN}✅ ElasticSearch 运行中${NC}"
    else
        echo -e "  ${YELLOW}⚠️  ES         未启动 (搜索功能暂不可用)${NC}"
    fi
    echo ""
    if [ "$ok" = false ]; then
        echo -e "${YELLOW}提示: 请先启动 MySQL 和 Redis${NC}"
        echo ""
    fi
}

# ====== ES 操作 ======

es_start() {
    if [ ! -d "$ES_HOME" ]; then
        echo -e "  ${YELLOW}⚠️  ES 安装目录 $ES_HOME 不存在，跳过自动启动${NC}"
        return
    fi
    echo -e "  ${YELLOW}⏳ 正在启动 ElasticSearch...${NC}"

    # 通过 cmd 启动 ES，清除有问题的环境变量
    cmd //c "set JAVA_HOME=&set CLASSPATH=.&set ES_JAVA_HOME=D:\\elasticsearch-9.4.2\\jdk&D:\\elasticsearch-9.4.2\\bin\\elasticsearch.bat" > /dev/null 2>&1 &
    local pid=$!
    echo "$pid" > "$ES_PID_FILE" || true

    # 等待 ES 就绪（最多 60 秒）
    local timeout=60
    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        if curl -s http://localhost:9200 &>/dev/null; then
            echo -e "  ${GREEN}✅ ElasticSearch 启动成功! PID: $(cat "$ES_PID_FILE")${NC}"
            return
        fi
        sleep 3
        elapsed=$((elapsed + 3))
    done
    echo -e "  ${YELLOW}⚠️  ES 启动超时，请手动启动（双击桌面 start-es.bat）${NC}"
}

es_stop() {
    if [ -f "$ES_PID_FILE" ]; then
        local pid=$(cat "$ES_PID_FILE")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 2
        fi
        rm -f "$ES_PID_FILE" || true
    fi
    # 额外检查 9200 端口
    local port_pid=$(netstat -ano 2>/dev/null | grep ":9200 " | grep LISTENING | awk '{print $NF}' | head -1)
    if [ -n "$port_pid" ] && [ "$port_pid" != "0" ]; then
        taskkill //F //PID "$port_pid" 2>/dev/null || kill "$port_pid" 2>/dev/null || true
        sleep 2
        echo -e "  ${GREEN}✅ ES 已停止${NC}"
    fi
}

# ====== 后端操作 ======

backend_start() {
    echo -e "${CYAN}══════ 启动后端 ══════${NC}"

    check_deps "backend"

    # 检查端口
    if netstat -ano 2>/dev/null | grep ":8080 " | grep LISTENING &>/dev/null; then
        echo -e "${YELLOW}⚠️  端口 8080 已被占用，后端可能已在运行${NC}"
        echo -e "  执行 $0 stop backend 后再试"
        return
    fi

    # 编译
    echo -e "${YELLOW}[1/2] 编译打包...${NC}"
    cd "$BACKEND_DIR"
    if mvn clean package -DskipTests -q; then
        echo -e "  ${GREEN}✅ 编译成功${NC}"
    else
        echo -e "${RED}[ERROR] 编译失败，请检查代码错误${NC}"
        exit 1
    fi

    # 加载 .env 文件（如果存在）
    if [ -f "$PROJECT_ROOT/.env" ]; then
        set -a; source "$PROJECT_ROOT/.env"; set +a
        echo -e "${GREEN}  [已加载 .env 文件]${NC}"
    fi

    # 启动
    echo -e "${YELLOW}[2/2] 启动服务...${NC}"
    nohup java -jar target/ocean-server-1.0.0.jar > "$BACKEND_LOG" 2>&1 &
    local pid=$!
    echo "$pid" > "$BACKEND_PID_FILE"

    # 等待启动完成
    local timeout=60
    local elapsed=0
    echo -n "  等待启动"
    while [ $elapsed -lt $timeout ]; do
        if grep -q "Started OceanApplication" "$BACKEND_LOG" 2>/dev/null; then
            echo ""
            echo -e "  ${GREEN}✅ 后端启动成功! PID: $pid${NC}"
            echo -e "  API:     ${CYAN}http://localhost:8080${NC}"
            echo -e "  Swagger: ${CYAN}http://localhost:8080/doc.html${NC}"
            return
        fi
        if grep -q "Application run failed\|ERROR" "$BACKEND_LOG" 2>/dev/null; then
            echo ""
            echo -e "${RED}[ERROR] 启动失败，查看日志:${NC}"
            tail -20 "$BACKEND_LOG"
            exit 1
        fi
        echo -n "."
        sleep 2
        elapsed=$((elapsed + 2))
    done
    echo ""
    echo -e "${RED}⏰ 启动超时，请检查日志: tail -f $BACKEND_LOG${NC}"
}

backend_stop() {
    echo -e "${CYAN}══════ 停止后端 ══════${NC}"
    if [ -f "$BACKEND_PID_FILE" ]; then
        local pid=$(cat "$BACKEND_PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            sleep 2
            echo -e "  ${GREEN}✅ 后端已停止 (PID: $pid)${NC}"
        else
            echo -e "  ${YELLOW}⚠️  后端进程不存在 (PID: $pid)${NC}"
        fi
        rm -f "$BACKEND_PID_FILE"
    else
        # 按端口查找
        local port_pid=$(netstat -ano 2>/dev/null | grep ":8080 " | grep LISTENING | awk '{print $NF}')
        if [ -n "$port_pid" ]; then
            kill "$port_pid" 2>/dev/null
            sleep 2
            echo -e "  ${GREEN}✅ 后端已停止 (PID: $port_pid)${NC}"
        else
            echo -e "  ${YELLOW}⚠️  后端未在运行${NC}"
        fi
    fi
}

backend_status() {
    if [ -f "$BACKEND_PID_FILE" ] && kill -0 "$(cat "$BACKEND_PID_FILE")" 2>/dev/null; then
        echo -e "  ${GREEN}✅ 后端    运行中    PID: $(cat "$BACKEND_PID_FILE")    http://localhost:8080${NC}"
    elif netstat -ano 2>/dev/null | grep ":8080 " | grep LISTENING &>/dev/null; then
        local pid
        pid=$(netstat -ano 2>/dev/null | grep ":8080 " | grep LISTENING | awk '{print $NF}')
        echo -e "  ${GREEN}✅ 后端    运行中    PID: $pid    http://localhost:8080 (未通过脚本启动)${NC}"
    else
        echo -e "  ${RED}❌ 后端    未运行${NC}"
    fi
}

# ====== 前端操作 ======

frontend_start() {
    echo -e "${CYAN}══════ 启动前端 ══════${NC}"

    check_deps "frontend"

    # 检查端口，如被占用则自动释放
    if netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING &>/dev/null; then
        local fp
        fp=$(netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING | awk '{print $NF}')
        echo -e "  ${YELLOW}端口 3000 被占用 (PID: $fp)，正在释放...${NC}"
        taskkill //F //PID "$fp" 2>/dev/null || kill -9 "$fp" 2>/dev/null || true
        sleep 2
        if netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING &>/dev/null; then
            echo -e "${RED}[ERROR] 无法释放端口 3000${NC}"
            return
        fi
        echo -e "  ${GREEN}✅ 端口已释放${NC}"
    fi

    cd "$FRONTEND_DIR"

    # 安装依赖
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}[1/2] 安装依赖...${NC}"
        if npm install; then
            echo -e "  ${GREEN}✅ 依赖安装完成${NC}"
        else
            echo -e "${RED}[ERROR] 依赖安装失败${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}[1/2] 依赖已就绪${NC}"
    fi

    # 启动开发服务器（后台）
    echo -e "${YELLOW}[2/2] 启动开发服务器...${NC}"
    nohup npm run dev > "$PROJECT_DIR/ocean-web.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$FRONTEND_PID_FILE"

    sleep 4
    if kill -0 "$pid" 2>/dev/null; then
        echo -e "  ${GREEN}✅ 前端启动成功! PID: $pid${NC}"
        echo -e "  地址: ${CYAN}http://localhost:3000${NC}"
    else
        echo -e "${RED}[ERROR] 前端启动失败，查看日志:${NC}"
        tail -10 "$PROJECT_DIR/ocean-web.log"
    fi
}

frontend_stop() {
    echo -e "${CYAN}══════ 停止前端 ══════${NC}"
    if [ -f "$FRONTEND_PID_FILE" ]; then
        local pid=$(cat "$FRONTEND_PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            sleep 2
            echo -e "  ${GREEN}✅ 前端已停止 (PID: $pid)${NC}"
        else
            echo -e "  ${YELLOW}⚠️  前端进程不存在${NC}"
        fi
        rm -f "$FRONTEND_PID_FILE"
    else
        # 按端口查找
        local port_pid=$(netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING | awk '{print $NF}')
        if [ -n "$port_pid" ]; then
            kill "$port_pid" 2>/dev/null
            sleep 2
            echo -e "  ${GREEN}✅ 前端已停止 (PID: $port_pid)${NC}"
        else
            echo -e "  ${YELLOW}⚠️  前端未在运行${NC}"
        fi
    fi
}

frontend_status() {
    if [ -f "$FRONTEND_PID_FILE" ] && kill -0 "$(cat "$FRONTEND_PID_FILE")" 2>/dev/null; then
        echo -e "  ${GREEN}✅ 前端    运行中    PID: $(cat "$FRONTEND_PID_FILE")    http://localhost:3000${NC}"
    elif netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING &>/dev/null; then
        local fp
        fp=$(netstat -ano 2>/dev/null | grep ":3000 " | grep LISTENING | awk '{print $NF}')
        echo -e "  ${GREEN}✅ 前端    运行中    PID: $fp    http://localhost:3000 (未通过脚本启动)${NC}"
    else
        echo -e "  ${RED}❌ 前端    未运行${NC}"
    fi
}

# ====== 主逻辑 ======

ACTION="${1:-start}"
MODULE="${2:-all}"

case "$ACTION" in
    start)
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo -e "${CYAN}  海洋生物知识库 — 启动${NC}"
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo ""

        # 检查基础设施
        if [ "$MODULE" = "all" ] || [ "$MODULE" = "backend" ]; then
            echo -e "${YELLOW}检查基础设施...${NC}"
            check_infra
            # 自动启动 ES（如未运行）
            if ! curl -s http://localhost:9200 &>/dev/null; then
                es_start
            fi
        fi

        case "$MODULE" in
            all)
                backend_start
                echo ""
                frontend_start
                echo ""
                echo -e "${GREEN}══════════════════════════════════════════${NC}"
                echo -e "${GREEN}  🎉 全部启动完成!${NC}"
                echo -e "${GREEN}  前端: http://localhost:3000${NC}"
                echo -e "${GREEN}  后端: http://localhost:8080${NC}"
                echo -e "${GREEN}  Swagger: http://localhost:8080/doc.html${NC}"
                echo -e "${GREEN}══════════════════════════════════════════${NC}"
                ;;
            backend)
                backend_start
                ;;
            frontend)
                frontend_start
                ;;
            *)
                usage
                ;;
        esac
        ;;
    stop)
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo -e "${CYAN}  停止服务${NC}"
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo ""
        case "$MODULE" in
            all)
                backend_stop
                frontend_stop
                es_stop
                echo ""
                echo -e "${GREEN}✅ 已全部停止${NC}"
                ;;
            backend)
                backend_stop
                ;;
            frontend)
                frontend_stop
                ;;
            *)
                usage
                ;;
        esac
        ;;
    restart)
        echo -e "${CYAN}══════ 重启 ══════${NC}"
        case "$MODULE" in
            all)
                backend_stop
                sleep 2
                frontend_stop
                sleep 2
                es_stop
                sleep 2
                es_start
                backend_start
                echo ""
                frontend_start
                echo ""
                echo -e "${GREEN}✅ 重启完成${NC}"
                ;;
            backend)
                backend_stop
                sleep 2
                backend_start
                ;;
            frontend)
                frontend_stop
                sleep 2
                frontend_start
                ;;
            *)
                usage
                ;;
        esac
        ;;
    status)
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo -e "${CYAN}  服务状态${NC}"
        echo -e "${CYAN}══════════════════════════════════════════${NC}"
        echo ""
        backend_status
        frontend_status
        if curl -s http://localhost:9200 &>/dev/null; then
            if [ -f "$ES_PID_FILE" ]; then
                echo -e "  ${GREEN}✅ ES       运行中    PID: $(cat "$ES_PID_FILE")    http://localhost:9200${NC}"
            else
                echo -e "  ${GREEN}✅ ES       运行中    http://localhost:9200 (未通过本脚本启动)${NC}"
            fi
        else
            echo -e "  ${RED}❌ ES       未运行${NC}"
        fi
        echo ""
        if netstat -ano 2>/dev/null | grep ":8080 " | grep LISTENING &>/dev/null; then
            echo -e "${GREEN}总阅读量: $(curl -s http://localhost:8080/api/snapshot/get-statistic 2>/dev/null | grep -o '"totalViewCount":[0-9]*' | cut -d: -f2)${NC}"
        fi
        ;;
    *)
        usage
        ;;
esac

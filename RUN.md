# 海洋生物知识库 — 项目启动指南

## 环境依赖

| 组件 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 1.8+ | 后端运行环境 |
| Maven | 3.6+ | 后端编译打包 |
| Node.js | 16+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 5.x | 缓存 / 去重 / 计数 |
| ElasticSearch | 7.17+ | 全文搜索 |
| RocketMQ | 5.x（可选） | 投票消息广播 |

---

## 一、快速启动（本地开发模式）

### 1. 启动基础设施

```bash
# MySQL（如未启动）
net start mysql
# 或通过 Docker
docker run -d --name ocean-mysql -e MYSQL_ROOT_PASSWORD=smj041209 -p 3306:3306 mysql:8.0

# Redis
redis-server
# 或通过 Docker
docker run -d --name ocean-redis -p 6379:6379 redis:5-alpine

# ElasticSearch
# Windows: 直接运行 elasticsearch.bat
# Docker: docker run -d --name ocean-es -p 9200:9200 -e "discovery.type=single-node" elasticsearch:7.17.0

# RocketMQ（可选，不影响核心功能）
# 见 RocketMQ 官方文档启动 nameserver + broker
```

### 2. 初始化数据库

```bash
mysql -u root -p < ocean-server/src/main/resources/sql/init.sql
mysql -u root -p < ocean-server/src/main/resources/sql/test_data.sql
```

### 3. 启动后端

```bash
# 编译打包
cd ocean-server
mvn clean package -DskipTests

# 运行（默认使用 application.yml 配置）
java -jar target/ocean-server-1.0.0.jar

# 如需生产环境配置
java -jar target/ocean-server-1.0.0.jar --spring.profiles.active=prod
```

> 后端默认运行在 `http://localhost:8080`

### 4. 启动前端

```bash
cd ocean-web
npm install
npm run dev
```

> 前端默认运行在 `http://localhost:3000`，API 自动代理到 `localhost:8080`

### 5. 访问

| 入口 | 地址 |
|------|------|
| 前端首页 | http://localhost:3000 |
| 后端 API | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/doc.html |

---

## 二、Docker 部署模式

### 1. 环境变量配置

复制配置模板并填写：

```bash
# Windows (PowerShell)
$env:DEEPSEEK_API_KEY="your-api-key"

# Linux/Mac
export DEEPSEEK_API_KEY="your-api-key"
```

### 2. 构建前端

```bash
cd ocean-web
npm install
npm run build
# 生成 dist/ 目录，供 Nginx 使用
```

### 3. 一键启动

```bash
docker-compose up -d
```

此命令会启动 7 个服务：

| 服务 | 容器名 | 端口 |
|------|--------|------|
| MySQL | ocean-mysql | 3306 |
| Redis | ocean-redis | 6379 |
| ElasticSearch | ocean-es | 9200 |
| RocketMQ NameServer | ocean-rocketmq-nameserver | 9876 |
| RocketMQ Broker | ocean-rocketmq-broker | 10911 |
| 后端 | ocean-server | 8080 |
| Nginx | ocean-nginx | 80 |

### 4. 访问

| 入口 | 地址 |
|------|------|
| 前端 + API | http://localhost |
| Swagger 文档 | http://localhost/doc.html |

### 5. 常用 Docker 命令

```bash
# 查看日志
docker logs -f ocean-server

# 重启单个服务
docker restart ocean-server

# 重新构建并启动
docker-compose up -d --build ocean-server

# 停止所有
docker-compose down

# 停止并删除数据卷（清空数据库）
docker-compose down -v
```

---

## 三、环境变量说明

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | `localhost` | MySQL 主机地址 |
| `MYSQL_PASSWORD` | `smj041209` | MySQL 密码 |
| `REDIS_HOST` | `localhost` | Redis 主机地址 |
| `ES_HOST` | `localhost` | ElasticSearch 主机地址 |
| `ROCKETMQ_HOST` | `localhost` | RocketMQ 主机地址 |
| `JWT_SECRET` | `ocean_knowledge_jwt_secret_2026` | JWT 签名密钥 |
| `DEEPSEEK_API_KEY` | （空） | DeepSeek API 密钥 |
| `MYBATIS_LOG` | `StdOutImpl` | MyBatis SQL 日志实现 |
| `UPLOAD_PATH` | `./upload/` | 文件上传路径 |

---

## 四、管理员账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 管理员 |

---

## 五、项目结构速览

```
ocean-knowledge-base/
├── ocean-server/                # 后端 Spring Boot 项目
│   ├── src/main/java/com/ocean/
│   │   ├── ai/                  # AI 集成（DeepSeek）
│   │   ├── common/              # 公共类（响应封装、异常、常量）
│   │   ├── config/              # 配置类（CORS、ES、MyBatis、Redis、WebSocket）
│   │   ├── controller/          # REST API 控制器
│   │   ├── domain/              # 实体类 + DTO
│   │   ├── interceptor/         # 拦截器（登录/管理/限流/日志）
│   │   ├── job/                 # 定时任务（数据快照）
│   │   ├── mapper/              # MyBatis-Plus 映射接口
│   │   ├── mq/                  # RocketMQ 消费者
│   │   ├── service/             # 业务逻辑层
│   │   ├── util/                # 工具类（JWT、BCrypt、XSS过滤）
│   │   ├── websocket/           # WebSocket 服务端
│   │   └── OceanApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml      # 主配置
│   │   ├── application-prod.yml # 生产配置
│   │   └── sql/
│   │       ├── init.sql         # 数据库建表 + 初始化数据
│   │       └── test_data.sql    # 测试数据
│   └── pom.xml
├── ocean-web/                   # 前端 Vue 3 + TypeScript 项目
│   ├── src/
│   │   ├── api/                 # Axios API 封装
│   │   ├── router/              # Vue Router 路由
│   │   ├── store/               # Vuex 状态管理
│   │   ├── views/               # 页面组件
│   │   │   ├── admin/           # 管理后台页面
│   │   │   └── error/           # 404/500 错误页
│   │   └── main.ts
│   └── package.json
├── docker-compose.yml           # Docker 编排
├── nginx.conf                   # Nginx 反向代理配置
└── RUN.md                       # 本文件
```

---

## 六、常见问题

### Q: 启动后端时报 `RocketMQ` 连接失败
不影响核心功能。如果没有启动 RocketMQ，可以在 `application.yml` 中注释掉 RocketMQ 配置，或确认 `VoteMessageConsumer.java` 中的依赖条件不满足即可。

### Q: ES 版本不匹配
实际使用的 ES 版本可能高于 7.17，系统通过 HTTP REST API 调用 ES，功能正常。启动日志中的 `Version mismatch` 警告不影响使用。

### Q: 前端页面显示空白
确认已执行 `npm install`，且后端已经启动。查看浏览器控制台是否有跨域或 API 错误。

### Q: 图片上传后无法访问
确认 `file.upload-path` 配置的目录存在且有写入权限。默认路径为 `./upload/`（相对于后端工作目录），Nginx 代理路径为 `/files/`。

### Q: 数据库初始化失败
1. 确认 MySQL 已启动
2. 执行 `init.sql` 前确保用户有建库权限
3. 测试数据 `test_data.sql` 在 `init.sql` 之后执行

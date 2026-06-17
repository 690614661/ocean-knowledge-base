# 海洋生物知识库 — 运行说明

## 系统概述

基于 Vue 3 + Spring Boot 的海洋生物在线知识库系统，支持知识浏览、全文检索、文档管理、AI 问答、数据统计和实时通知。

**广东海洋大学 实训项目 · 第 20 组**

---

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 11+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建工具 |
| Node.js | 16+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 5.x | 缓存和 Token 存储 |
| ElasticSearch | 7.17（可选） | 全文检索 |

---

## 快速启动（本地开发）

### 1. 启动依赖服务

确保 MySQL 和 Redis 已启动：

```bash
# MySQL 连接信息
主机: localhost:3306
用户名: root
密码: 按实际配置
数据库: ocean_knowledge
```

### 2. 初始化数据库

```bash
mysql -u root -p < ocean-server/src/main/resources/sql/init.sql
```

脚本会自动创建 `ocean_knowledge` 数据库，包含用户、分类、电子书、文档、笔记等全部数据表，并初始化演示数据。

### 3. 启动后端

```bash
# 编译
mvn clean package -DskipTests -f ocean-server/pom.xml

# 运行（指定 MySQL 密码）
MYSQL_PASSWORD=你的密码 java -jar ocean-server/target/ocean-server-1.0.0.jar --spring.profiles.active=dev
```

后端启动后监听 **8080 端口**，可通过 http://localhost:8080/doc.html 访问 Swagger 接口文档。

### 4. 启动前端

```bash
cd ocean-web
npm install
npm run dev
```

前端启动后访问 **http://localhost:3000**，自动代理 `/api` 请求到后端。

---

## Docker 部署

项目根目录提供了 `docker-compose.yml`，一键启动所有服务：

```bash
# 启动所有容器
docker compose up -d

# 查看运行状态
docker compose ps

# 查看日志
docker compose logs -f ocean-server

# 停止所有容器
docker compose down

# 重建并启动（代码变更后）
docker compose up -d --build ocean-server
```

Docker Compose 会依次启动：MySQL → Redis → ElasticSearch → RocketMQ → 后端 → Nginx（前端）。

| 服务 | 容器名 | 端口 |
|------|--------|------|
| Nginx（前端） | ocean-nginx | 80 |
| 后端 API | ocean-server | 8080 |
| MySQL | ocean-mysql | 3306 |
| Redis | ocean-redis | 6379 |
| ElasticSearch | ocean-es | 9200 |
| RocketMQ | ocean-rocketmq-nameserver | 9876 |

---

## 配置说明

### 后端配置

配置文件位于 `ocean-server/src/main/resources/`：

| 文件 | 用途 |
|------|------|
| `application.yml` | 公共配置（默认 dev 环境） |
| `application-dev.yml` | 本地开发配置 |
| `application-prod.yml` | Docker 生产配置 |

关键配置项：

```yaml
# MySQL 连接
spring.datasource.url: jdbc:mysql://localhost:3306/ocean_knowledge
spring.datasource.username: root
spring.datasource.password: ${MYSQL_PASSWORD:root}

# Redis
spring.redis.host: localhost
spring.redis.port: 6379

# JWT 密钥
jwt.secret: ${JWT_SECRET:ocean_knowledge_jwt_secret_2026}
jwt.expiration: 86400000          # 24 小时

# 密码盐值
salt.password: ocean_knowledge_salt_2026

# AI 接口（可选）
ai.deepseek.api-key: ${DEEPSEEK_API_KEY:}
```

### 前端配置

| 文件 | 说明 |
|------|------|
| `ocean-web/vue.config.js` | 开发服务器端口、API 代理地址 |

前端开发服务器默认 **3000 端口**，代理 `/api` → `localhost:8080`，`/ws` → `ws://localhost:8080`。

---

## 测试账号

| 角色 | 登录名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |

---

## 项目结构

```
ocean-knowledge-base/
├── ocean-server/          # 后端 Spring Boot
│   └── src/main/
│       ├── java/          # Java 源码
│       └── resources/     # 配置文件和 SQL
├── ocean-web/             # 前端 Vue 3
│   └── src/
│       ├── views/         # 页面组件
│       ├── api/           # API 封装
│       ├── router/        # 路由
│       └── store/         # 状态管理
├── docs/                  # 设计文档
├── docker-compose.yml     # 容器编排
└── README.md
```

---

## 常见问题

### Q: 后端启动报端口被占用
```bash
# 查找占用 8080 端口的进程
netstat -ano | grep :8080
# 在 Windows 上强制终止
taskkill //F //PID 进程号
```

### Q: MySQL 连接失败
检查 `application-dev.yml` 中的密码是否与本机 MySQL 密码一致，或通过环境变量指定：
```bash
MYSQL_PASSWORD=你的密码 java -jar ocean-server-1.0.0.jar
```

### Q: IDEA 无法直接运行 OceanApplication
- 确保安装了 **Lombok 插件**
- 开启注解处理：`Settings → Build → Compiler → Annotation Processors → Enable annotation processing`
- 如需用 IDEA 高版本 JDK，pom.xml 已包含 jaxb-runtime 依赖兼容

### Q: 前端页面刷新后 404
这是 Vue Router history 模式的特性，开发服务器已配置 `historyApiFallback: true`。如部署到 Nginx，需在配置中添加：
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### Q: 登录提示"数据冲突"
BCrypt 密码长度超过数据库 `VARCHAR(32)` 限制，执行以下 SQL 扩展字段：
```sql
ALTER TABLE `user` MODIFY `password` VARCHAR(100) NOT NULL;
```

# 海洋生物知识库 - 启动说明

## 1. 环境要求

| 组件 | 版本要求 |
|------|---------|
| JDK | 1.8 |
| Maven | 3.6+ |
| Node.js | 16+ |
| MySQL | 8.0 |
| Redis | 5.x |
| ElasticSearch | 7.17.x |
| RocketMQ | 5.1.x |

## 2. 方式一：本地开发启动

### 2.1 启动中间件

先确保本地已安装并启动 MySQL、Redis、ElasticSearch、RocketMQ。

### 2.2 初始化数据库

```bash
# 登录 MySQL，执行初始化脚本
mysql -u root -p < ocean-server/src/main/resources/sql/init.sql
```

脚本会自动创建 `ocean_knowledge` 数据库、9 张表，并插入管理员初始账号。

### 2.3 配置后端

编辑 `ocean-server/src/main/resources/application-dev.yml`，按实际环境修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ocean_knowledge?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 你的MySQL密码    # 默认 root
  redis:
    host: localhost
    port: 6379
  elasticsearch:
    uris: http://localhost:9200
  rocketmq:
    name-server: localhost:9876
```

**AI 功能配置**（可选，不配置时 AI 接口会报错但不影响其他功能）：

```yaml
ai:
  deepseek:
    api-key: 你的DeepSeek API Key
```

也可通过环境变量设置：`set DEEPSEEK_API_KEY=sk-xxx`

### 2.4 启动后端

```bash
cd ocean-server
mvn spring-boot:run
```

后端启动成功后访问：`http://localhost:8080`

Swagger API 文档：`http://localhost:8080/doc.html`

### 2.5 启动前端

```bash
cd ocean-web
npm install
npm run dev
```

前端开发服务器启动后访问：`http://localhost:3000`

## 3. 方式二：Docker Compose 一键部署

### 3.1 构建前端

```bash
cd ocean-web
npm install
npm run build
cd ..
```

### 3.2 构建后端 JAR

```bash
cd ocean-server
mvn clean package -DskipTests
cd ..
```

### 3.3 配置 AI Key（可选）

```bash
# Windows PowerShell
$env:DEEPSEEK_API_KEY="sk-xxx"

# Linux/Mac
export DEEPSEEK_API_KEY=sk-xxx
```

### 3.4 一键启动

```bash
docker-compose up -d
```

启动完成后访问：`http://localhost`

### 3.5 查看日志

```bash
# 查看所有服务状态
docker-compose ps

# 查看后端日志
docker-compose logs -f ocean-server

# 停止所有服务
docker-compose down
```

## 4. 默认账号

| 角色 | 登录名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

> 密码使用 MD5 + 盐值 `ocean_knowledge_salt_2026` 加密存储。

## 5. 服务端口一览

| 服务 | 本地开发端口 | Docker 端口 |
|------|------------|------------|
| 前端 (Vue) | 3000 | 80 (Nginx) |
| 后端 (Spring Boot) | 8080 | 8080 |
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |
| ElasticSearch | 9200 | 9200 |
| RocketMQ NameServer | 9876 | 9876 |
| RocketMQ Broker | 10911 | 10911 |

## 6. ElasticSearch 索引初始化

首次使用全文检索功能前，需手动创建 ES 索引：

```bash
curl -X PUT "http://localhost:9200/doc_index" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "name": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "content": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "ebookId": { "type": "long" }
    }
  }
}'
```

> 需要 ES 安装 IK 分词器插件。如未安装，可将 analyzer 改为 `standard`：
> ```bash
> curl -X PUT "http://localhost:9200/doc_index" -H "Content-Type: application/json" -d '{
>   "mappings": {
>     "properties": {
>       "name": { "type": "text" },
>       "content": { "type": "text" },
>       "ebookId": { "type": "long" }
>     }
>   }
> }'
> ```

## 7. 常见问题

### Q: 后端启动报 `Communications link failure`
**A**: MySQL 未启动或连接配置错误，检查 `application-dev.yml` 中的数据库地址和密码。

### Q: 后端启动报 `Unable to connect to Redis`
**A**: Redis 未启动，执行 `redis-server` 启动 Redis。

### Q: AI 问答功能报错
**A**: 需要配置 DeepSeek API Key。在 `application-dev.yml` 中设置 `ai.deepseek.api-key`，或通过环境变量 `DEEPSEEK_API_KEY` 传入。不配置时其他功能正常使用。

### Q: 搜索功能报错
**A**: 需要启动 ElasticSearch 并创建 `doc_index` 索引（见第 6 节）。

### Q: Docker 启动后 MySQL 初始化失败
**A**: 如果数据卷已存在旧数据，MySQL 不会重新执行 init.sql。执行 `docker-compose down -v` 删除数据卷后重新启动。

### Q: 前端页面空白
**A**: 确保后端已启动，且前端代理配置正确。开发模式下前端通过 `vue.config.js` 中的 proxy 将 `/api` 请求代理到 `http://localhost:8080`。

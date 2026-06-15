# 海洋生物知识库 架构边界

## 1. 系统架构总览

```
┌─────────────────────────────────────────────────────┐
│                    Nginx (前端静态资源)                │
│                   http://localhost                   │
└──────────────────────┬──────────────────────────────┘
                       │ API 代理
┌──────────────────────▼──────────────────────────────┐
│              Spring Boot 后端 (8080)                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │Controller│→│ Service  │→│ Mapper   │→│ MySQL  │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ │
│       │              │             │                 │
│       │              │             └→ Redis (缓存)   │
│       │              └→ ElasticSearch (全文检索)      │
│       └→ WebSocket Server                            │
│              └→ RocketMQ (异步通知)                   │
│       ┌→ AI Module                                    │
│       │  └→ AiProvider (抽象层)                       │
│       │     └→ DeepSeek API (外部)                    │
└─────────────────────────────────────────────────────┘
```

## 2. 应用边界

### 前端应用（ocean-web）
- **职责**：UI 渲染、用户交互、路由管理、状态管理
- **技术栈**：Vue 3 + TypeScript + Ant Design Vue 4.x
- **构建工具**：Vue CLI
- **部署方式**：Nginx 静态资源服务

### 后端应用（ocean-server）
- **职责**：业务逻辑、数据持久化、安全认证、定时任务、消息处理
- **技术栈**：Spring Boot 2.4.0 + MyBatis-Plus 3.5.2
- **部署方式**：Docker 容器
- **API 文档**：knife4j（Swagger 增强），访问地址 `/doc.html`

### 数据层
- **MySQL 8.0**：主数据存储
- **Redis 5.x**：缓存 + 点赞防重 + Token 存储
- **ElasticSearch**：全文检索索引

## 3. 模块边界（后端）

```
com.ocean
├── config/              # 全局配置（CORS、Swagger、WebSocket、MQ、AI）
├── common/              # 公共基础（CommonResp、全局异常、常量）
├── interceptor/         # 拦截器（日志、XSS、登录校验、限流）
├── controller/          # 接口层（按业务域划分）
├── service/             # 业务逻辑层
├── mapper/              # MyBatis-Plus Mapper
├── domain/              # 实体、请求对象、响应对象
├── websocket/           # WebSocket 服务
├── mq/                  # RocketMQ 消费者
├── job/                 # 定时任务
├── ai/                  # AI 模块（供应商抽象、编排、用量记录）
│   ├── provider/        # AiProvider 接口 + DeepSeek 实现
│   ├── model/           # AiRequest、AiResponse、ChatMessage
│   ├── service/         # AI 服务（编排、上下文构建、用量记录）
│   └── prompt/          # Prompt 模板管理
└── util/                # 工具类（JWT、Redis、MD5、雪花ID）
```

### 模块职责划分

| 模块 | 职责 | 对外接口 |
|------|------|---------|
| SwaggerConfig | API 文档配置（knife4j） | /doc.html |
| EbookController | 电子书 CRUD + 列表查询 | REST API |
| CategoryController | 分类 CRUD + 树形查询 | REST API |
| DocController | 文档 CRUD + 内容保存 | REST API |
| UserController | 用户管理 + 登录/退出 | REST API |
| SnapshotController | 统计数据查询 | REST API |
| FileController | 文件上传（封面图） | REST API |
| WebSocketServer | 点赞实时通知 | WebSocket |
| EbookSnapshotJob | 数据快照定时生成 | Cron Job |
| VoteMessageConsumer | 点赞通知异步消费 | RocketMQ Consumer |
| AiController | AI 问答 + 内容生成 | REST API |
| NoteController | 用户笔记 CRUD | REST API |
| DeepSeekProvider | DeepSeek API 调用 | 外部 HTTP |

## 4. 运行时边界

| 组件 | 容器 | 端口 | 说明 |
|------|------|------|------|
| Nginx | Docker | 80 | 前端静态资源 + API 反向代理 |
| Spring Boot | Docker | 8080 | 后端 API 服务 |
| MySQL | Docker | 3306 | 数据库 |
| Redis | Docker | 6379 | 缓存 |
| ElasticSearch | Docker | 9200 | 全文检索 |
| RocketMQ NameServer | Docker | 9876 | 消息队列注册中心 |
| RocketMQ Broker | Docker | 10911 | 消息队列 Broker |

### Docker Compose 编排

```yaml
services:
  mysql:       image: mysql:8.0
  redis:       image: redis:5-alpine
  elasticsearch: image: elasticsearch:7.17.0
  rocketmq-nameserver: image: apache/rocketmq:5.1.4
  rocketmq-broker:     image: apache/rocketmq:5.1.4
  ocean-server:  build from Dockerfile (Java 8+)
  nginx:         image: nginx:alpine
```

## 5. 集成边界

### 前端 → 后端
- **协议**：HTTP REST API（JSON）
- **认证**：Header `token: <jwt-value>`
- **跨域**：后端 CORS 配置允许前端域名

### 后端 → MySQL
- **ORM**：MyBatis-Plus 3.5.2
- **连接池**：HikariCP（Spring Boot 默认）
- **查询方式**：参数化查询，禁止拼接 SQL

### 后端 → Redis
- **客户端**：Spring Data Redis / Jedis
- **用途**：Token 存储、点赞防重、热点数据缓存
- **Key 设计**：
  - Token: `token:{userId}` → JWT string
  - 点赞防重: `vote:{docId}:{ip}:{date}` → "1"
  - 缓存: `cache:ebook:list`、`cache:doc:tree:{ebookId}`

### 后端 → ElasticSearch
- **客户端**：Spring Data Elasticsearch
- **索引**：`doc_index`（doc_id, name, content, ebook_id）
- **同步策略**：文档增删改时同步写 ES（Service 层调用）

### 后端 → RocketMQ
- **Producer**：点赞成功后发送消息
- **Consumer**：消费消息，通过 WebSocket 推送通知
- **Topic**：`VOTE_TOPIC`

### 后端 → DeepSeek API
- **协议**：HTTPS REST API（JSON）
- **认证**：API Key（环境变量，不硬编码）
- **超时**：60 秒
- **重试**：失败不自动重试（避免重复计费）
- **限流**：每用户每分钟 10 次，每天 100 次

## 6. 高风险能力边界

| 能力 | 风险 | 控制措施 |
|------|------|---------|
| 用户密码 | 泄露 | MD5+盐值加密，不返回前端，日志不打印 |
| 富文本内容 | XSS | 白名单过滤，仅允许安全标签 |
| 接口访问 | 未授权 | JWT Token + 拦截器校验 |
| 点赞接口 | 刷票 | Redis 防重 + 接口限流 |
| 登录接口 | 暴力破解 | 接口限流（每分钟 10 次） |
| 数据库 | SQL 注入 | MyBatis-Plus 参数化查询 |
| AI API Key | 泄露 | 环境变量存储，不返回前端，日志不打印 |
| AI 调用 | 费用超支 | 接口限流 + 每日上限 + 用量日志 |
| AI 输出 | 敏感内容 | 输出校验 + 管理员确认机制 |

## 7. 技术栈确认

| 层次 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 3.x |
| UI 组件库 | Ant Design Vue | 4.x |
| 构建工具 | Vue CLI | 5.x |
| 图表库 | ECharts | 5.x |
| 富文本编辑器 | wangEditor | 5.x |
| 后端框架 | Spring Boot | 2.4.0 |
| ORM | MyBatis-Plus | 3.5.2 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 5.x |
| 搜索引擎 | ElasticSearch | 7.17 |
| 消息队列 | RocketMQ | 5.1.4 |
| 项目管理 | Maven | 3.x |
| 版本控制 | Git | - |
| 部署 | Docker Compose | - |
| AI 供应商 | DeepSeek API | deepseek-chat |
| HTTP 客户端 | OkHttp / RestTemplate | 调用外部 AI API |

## 8. 前端模块结构

```
ocean-web/src/
├── views/           # 页面（Home、EbookList、EbookDetail、Login、admin/*、AiChat、NoteList、NoteEdit）
├── components/      # 组件（EbookCard、DocTree、WangEditor、StatCard、AiChatPanel、NoteEditor）
├── api/             # 接口封装（request.ts、ebook.ts、category.ts、doc.ts、ai.ts、note.ts）
├── router/          # 路由 + 守卫
├── store/           # Vuex 状态管理
├── utils/           # 工具（WebSocket）
└── assets/          # 静态资源
```

## 9. 开放问题

| 问题 | 影响 | 建议 |
|------|------|------|
| ES 版本选择 | 兼容性 | 使用 7.17（稳定版），避免 8.x 的 breaking changes |
| RocketMQ 是否必须 | 部署复杂度 | 可先用内存队列替代，答辩前再接入 RocketMQ |
| 文件上传存储 | 本地 vs OSS | V1 使用本地 Docker Volume 挂载 |

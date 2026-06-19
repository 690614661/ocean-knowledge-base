# 🌊 Ocean Knowledge Base — 海洋生物知识库

基于 Vue 3 + Spring Boot 的海洋生物在线知识库系统，支持 AI 智能问答、全文检索、文档管理、数据统计和实时通知。采用微服务架构设计，集成 Docker Compose 一键部署。

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Ant Design Vue 4.x + ECharts + wangEditor |
| 后端 | Spring Boot 2.4 + MyBatis-Plus 3.5 + Spring AOP |
| 数据库 | MySQL 8.0 + Redis 5.x + ElasticSearch 9.x |
| 消息队列 | RocketMQ 5.x + WebSocket |
| AI | 阿里云百炼（通义千问）API，SSE 流式输出 |
| 部署 | Docker Compose（7 容器编排）+ Nginx 反向代理 |

## 功能特性

### 🤖 AI 智能交互
- **AI 问答**：多轮对话，支持上下文连续问答
- **流式输出（SSE）**：实时打字机效果，逐字显示回复
- **AI 辅助笔记**：AI 生成/扩写/总结/润色学习笔记
- **AI 辅助写作**：管理员撰写文档时的 AI 辅助功能
- **供应商抽象**：支持多 AI 供应商切换（DeepSeek / 通义千问）

### 📚 知识管理
- **电子书 + 文档**：两级内容组织，树形目录结构
- **富文本编辑**：基于 wangEditor 5.x 的在线编辑器
- **全文检索**：ElasticSearch 关键词搜索 + 搜索结果高亮
- **文档收藏**：收藏/取消收藏，个人收藏夹管理
- **用户笔记**：创建/编辑/分享学习笔记（支持公开/私有）

### 👤 个人中心
- 个人信息编辑、密码修改
- 收藏列表管理
- 浏览历史记录（Redis 持久化，最多 50 条）

### 📊 数据统计
- 总阅读/总点赞/今日数据/环比增长率统计卡片
- 30 天阅读/点赞趋势折线图（ECharts）
- 电子书数据每日快照（定时任务，分钟级）

### 🛡️ 安全防护
- JWT + Redis 双重认证
- XSS 过滤（Jsoup 白名单）
- 接口限流（@RateLimit 注解）
- SQL 注入防御（MyBatis-Plus 参数化查询）
- 密码加密（BCrypt）

## 项目结构

```
ocean-knowledge-base/
├── ocean-server/          # 后端 Spring Boot
│   ├── controller/        # REST API 控制器
│   ├── service/           # 业务逻辑层
│   ├── mapper/            # MyBatis-Plus 数据访问
│   ├── ai/                # AI 模块（供应商抽象层）
│   ├── config/            # 全局配置（CORS/WebSocket/ES/Redis）
│   └── websocket/         # WebSocket 实时通知
├── ocean-web/             # 前端 Vue 3 + TypeScript
│   ├── views/             # 页面组件（首页/阅读/AI/笔记/管理后台）
│   └── api/               # Axios 接口封装
├── docs/                  # 完整设计文档（PRD/架构/API/数据模型）
├── docker-compose.yml     # 7 服务容器编排
└── start.sh               # 一键启动脚本（含 ES 自动启动）

```

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
docker-compose up -d
```

### 方式二：本地开发

```bash
git clone https://github.com/690614661/ocean-knowledge-base.git
cd ocean-knowledge-base

# 启动（自动检查依赖、启动 ES、编译后端、启动前端）
./start.sh start all
```

访问地址：
| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:3000 |
| 后端 API | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/doc.html |

## AI 配置

在 `application.yml` 或环境变量中配置 AI 供应商：

```bash
export BAILIAN_API_KEY=your_api_key_here
export AI_PROVIDER=bailian    # 支持 bailian / deepseek
```

## 🤝 联系方式

[GitHub](https://github.com/690614661) · 个人项目，持续维护中

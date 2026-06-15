# Ocean Knowledge Base — 海洋生物知识库

基于 Vue3 + Spring Boot 的海洋生物在线知识库系统，支持知识浏览、全文检索、文档管理、数据统计和实时通知。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Ant Design Vue | 4.x |
| 后端 | Spring Boot + MyBatis-Plus | 2.4.0 / 3.5.2 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 5.x |
| 搜索 | ElasticSearch | 7.17 |
| 消息队列 | RocketMQ | 5.1.4 |
| 富文本 | wangEditor | 5.x |
| 图表 | ECharts | 5.x |
| 部署 | Docker Compose | - |

## 项目结构

```
ocean-knowledge-base/
├── ocean-server/          # 后端 Spring Boot
├── ocean-web/             # 前端 Vue3
├── docs/                  # 架构文档
│   ├── PROJECT_BRIEF.md
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── DATA_MODEL.md
│   ├── API_CONTRACT.md
│   ├── PRIVACY_SECURITY.md
│   └── IMPLEMENTATION_READINESS.md
├── docker-compose.yml     # 容器编排
└── README.md
```

## 核心功能

- **知识浏览**：电子书列表 → 文档目录树 → 富文本阅读
- **全文检索**：ElasticSearch 关键词搜索 + 高亮
- **文档点赞**：Redis 防重（同 IP 每天一次）+ WebSocket 实时通知
- **数据统计**：每日快照、首页统计卡片、30 天 ECharts 趋势图
- **后台管理**：电子书/分类/文档/用户的增删改查
- **安全防护**：JWT 认证、XSS 过滤、接口限流、密码 MD5+盐值加密

## 快速启动

```bash
# 启动所有服务
docker-compose up -d

# 访问
# 前端：http://localhost
# 后端 API：http://localhost:8080
# Swagger 文档：http://localhost:8080/doc.html
```

## 开发环境

- JDK 8+
- Node.js 16+
- Maven 3.6+
- MySQL 8.0
- Redis 5.x
- ElasticSearch 7.17

## 团队

4 人开发团队，25 天开发周期。

## 许可证

实训项目，仅供学习使用。

# 🌊 Ocean Knowledge Base — 海洋生物知识库

基于 Vue 3 + Spring Boot 的海洋生物在线知识库系统，集成 AI 图像识别/问答、全文检索、文档管理、数据统计和实时通知。

> 📌 **个人全栈项目** — 完整实现 AI 集成、系统架构设计、前后端开发

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Ant Design Vue 4.x + ECharts + wangEditor + marked |
| 后端 | Spring Boot 2.4 + MyBatis-Plus 3.5 + Spring AOP + WebSocket |
| 数据库 | MySQL 8.0 + Redis 5.x + ElasticSearch 9.x |
| 消息队列 | RocketMQ 5.x + WebSocket |
| 文件存储 | 七牛云对象存储 + 本地文件存储（策略模式切换） |
| AI | 阿里云百炼（通义千问 qwen-plus + qwen-vl-plus 多模态） |
| 部署 | Docker Compose + Nginx 反向代理 |

## 功能特性

### 🤖 AI 智能交互
- **AI 多模态图像识别**：上传海洋生物图片，AI 识别物种/分类/特征（通义千问 VL 多模态模型）
- **AI 问答**：多轮对话，支持上下文连续问答
- **流式输出（SSE）**：实时打字机效果，逐字显示回复
- **AI 辅助笔记**：AI 生成/扩写/总结/润色学习笔记
- **供应商抽象**：支持多 AI 供应商切换（DeepSeek / 通义千问）

### 📚 知识管理
- **电子书 + 文档**：两级内容组织，树形目录结构
- **富文本编辑**：基于 wangEditor 5.x 的在线编辑器
- **全文检索**：ES 关键词搜索 + 分类/类型过滤 + 排序（相关度/时间/热度）+ 搜索结果高亮
- **文档版本管理**：版本快照，支持回退
- **文档评论/批注**：评论区 + 两级嵌套回复 + 用户头像
- **用户笔记**：创建/编辑/分享笔记（支持公开/私有），ES 全文搜索

### 👤 个人中心
- 个人信息编辑、头像上传（七牛云 CDN）
- 收藏列表管理（文档 + 笔记）
- 浏览历史记录（Redis 持久化，最多 50 条）
- 用户自助注册（邮箱验证码 + 密码强度校验）
- 登录日志 + 实时在线人数监控

### 🔔 消息通知
- **WebSocket 实时推送**：点赞/评论通知实时送达
- **通知持久化**：数据库存储 + 未读计数
- **导航栏小红点**：未读消息标记
- **通知列表**：标记已读/全部已读

### 📊 数据统计
- 总阅读/总点赞/今日数据/环比增长率统计卡片
- 30 天阅读/点赞趋势折线图（ECharts）
- 电子书数据每日快照

### 🛡️ 安全防护
- JWT + Redis 双重认证
- XSS 过滤（Jsoup 白名单）
- 接口限流（@RateLimit 注解）
- SQL 注入防御（MyBatis-Plus 参数化查询）
- 密码加密（BCrypt 优先，兼容旧 MD5）

## 快速启动

```bash
# 1. 配置环境变量（复制并修改）
cp .env.example .env

# 2. Docker Compose 部署
docker-compose up -d

# 3. 或本地开发启动
./start.sh start all
```

## 环境变量配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `BAILIAN_API_KEY` | 阿里云百炼 API Key | — |
| `AI_PROVIDER` | AI 供应商（bailian / deepseek） | bailian |
| `FILE_STORAGE_TYPE` | 文件存储（local / qiniu） | local |
| `QINIU_ACCESS_KEY` | 七牛云 Access Key | — |
| `QINIU_SECRET_KEY` | 七牛云 Secret Key | — |
| `QINIU_BUCKET` | 七牛云存储空间名 | ocean-knowledge-base1 |
| `QINIU_DOMAIN` | 七牛云 CDN 域名 | http://th48blda6.hn-bkt.clouddn.com |

---

[GitHub](https://github.com/690614661) · [Gitee](https://gitee.com/tianhua-yuan/ocean-knowledge-base) · 个人项目，持续维护中

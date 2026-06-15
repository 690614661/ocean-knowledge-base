# 海洋生物知识库 实现准备度检查

## 1. 架构产物检查

| 产物 | 状态 | 文件 |
|------|------|------|
| 项目总纲 | ✅ 完成 | docs/PROJECT_BRIEF.md |
| PRD / 范围文档 | ✅ 完成 | docs/PRD.md |
| 角色和权限 | ✅ 明确 | PRD §3, §4 |
| V1 范围和非目标 | ✅ 明确 | PRD §7, §8 |
| 核心流程 | ✅ 已定义 | PRD §5 |
| 状态机 | ✅ 已定义（简化版） | PRD §6 |
| 数据模型蓝图 | ✅ 完成 | docs/DATA_MODEL.md |
| 数据库 Schema 策略 | ✅ 明确 | DATA_MODEL §2 |
| API Contract | ✅ 完成 | docs/API_CONTRACT.md |
| AI / 自动化规格 | ✅ 完成 | docs/AGENT_SPEC.md |
| 隐私 / 安全 / 风险 | ✅ 完成 | docs/PRIVACY_SECURITY.md |
| 开放问题 | ✅ 已列出 | 各文档 §开放问题 |

**结论**：所有必须产物已就绪。

## 2. 进入实现的条件

| 条件 | 状态 | 说明 |
|------|------|------|
| 用户批准架构方向 | ✅ | Phase 1~9 均已确认 |
| 第一个构建切片已识别 | ✅ | P1：环境骨架 + 用户登录 |
| 技术栈已选择 | ✅ | Vue3 + Spring Boot + MySQL + Redis + ES |
| 仓库结构已知 | ✅ | ocean-server + ocean-web |
| 依赖策略已知 | ✅ | Maven（后端）+ npm（前端） |
| 验证策略已知 | ✅ | 交叉测试 + 安全测试 |
| Secrets 和环境处理 | ✅ | .env 文件 + Docker Compose 环境变量 |
| 数据 Migration 策略 | ✅ | 初始化 SQL 脚本 |

**结论**：所有进入条件已满足。

## 3. 实现路线图

### 阶段 1：基础骨架（Day 1~6）

**后端（A + B）**
- [ ] 创建 Gitee 仓库，初始化 Git
- [ ] 创建 Spring Boot 项目，配置 pom.xml 依赖
- [ ] 配置 application.yml（MySQL、Redis、ES 连接信息）
- [ ] 配置 Swagger/knife4j（@Api、@ApiOperation 注解）
- [ ] 实现 CommonResp 统一返回对象
- [ ] 实现全局异常处理（BusinessException + 参数校验异常）
- [ ] 实现 XSS 过滤器
- [ ] 实现 CORS 跨域配置
- [ ] 实现 AOP 日志 + 耗时监控（MDC LOG_ID）
- [ ] 创建所有数据库表（初始化 SQL）
- [ ] 实现用户模块：登录/退出/JWT/Redis Token
- [ ] 实现登录拦截器
- [ ] 实现接口限流（@RateLimit 注解）

**前端（C + D）**
- [ ] 创建 Vue3 项目，配置 TypeScript + ESLint
- [ ] 安装 Ant Design Vue 4.x
- [ ] 配置 Axios 实例 + 请求/响应拦截器（Token 注入、401 处理）
- [ ] 实现 Vuex 状态管理（用户信息、Token，存入 sessionStorage 持久化）
- [ ] 实现路由配置 + 路由守卫（未登录跳转首页）
- [ ] 实现登录页
- [ ] 实现导航栏动态菜单（根据登录状态和角色显示/隐藏管理入口）
- [ ] 实现 404/500 错误页面

### 阶段 2：核心业务（Day 7~14）

**后端（A）**
- [ ] 实现分类模块：CRUD + 树形查询
- [ ] 实现电子书模块：CRUD + 分页 + 模糊搜索 + 分类过滤
- [ ] 实现文件上传接口（封面图）
- [ ] 实现文档模块：CRUD + 树形查询 + 内容保存（doc + content）
- [ ] 实现文档递归删除
- [ ] 实现文档阅读接口（view_count +1）

**后端（B）**
- [ ] 搭建 ElasticSearch，创建 doc_index 索引
- [ ] 实现 ES 索引同步（文档增删改时）
- [ ] 实现全文检索接口（关键词高亮 + 分页）
- [ ] 实现 Redis 缓存（首页统计、电子书列表、文档目录树）
- [ ] 实现点赞接口（Redis 防重 + vote_count +1）

**前端（C）**
- [ ] 实现管理后台布局（AdminLayout）
- [ ] 实现电子书管理页（表格 + 新增/编辑弹窗 + 删除确认）
- [ ] 实现分类管理页（树形表格 + 新增/编辑弹窗）
- [ ] 实现文档管理页（树形表格 + wangEditor 富文本编辑器）
- [ ] 实现用户管理页（表格 + 新增/编辑弹窗 + 重置密码）

**前端（D）**
- [ ] 实现电子书列表页（卡片展示 + 分类筛选）
- [ ] 实现文档阅读页（目录树 + 富文本渲染 + 点赞按钮）
- [ ] 实现搜索功能（搜索框 + 搜索结果页 + 关键词高亮）

### 阶段 3：增强功能（Day 15~19）

**后端（A）**
- [ ] 实现 WebSocket 服务端（Session 管理 + 消息推送）
- [ ] 实现 RocketMQ Producer（点赞事件）
- [ ] 实现 RocketMQ Consumer（通知推送）

**后端（B）**
- [ ] 实现电子书快照定时任务（每分钟，幂等）
- [ ] 实现统计查询接口（总阅读/总点赞/今日数据/增长率/趋势）

**前端（C）**
- [ ] 管理页交互打磨（分页、搜索、表单校验、操作反馈）
- [ ] 响应式适配

**前端（D）**
- [ ] 实现首页统计卡片（StatCard 组件）
- [ ] 实现 ECharts 30 天趋势折线图
- [ ] 实现 WebSocket 通知 UI（消息铃铛/弹窗）
- [ ] 首页数据对接

### 阶段 4：测试部署（Day 20~25）

**全员**
- [ ] 交叉测试（A 测前端、C 测后端）
- [ ] 安全测试（XSS、SQL 注入、Token、限流）
- [ ] 性能测试（并发点赞、缓存命中）
- [ ] Bug 修复
- [ ] Docker Compose 编排（MySQL + Redis + ES + RocketMQ + 后端 + Nginx）
- [ ] 多环境配置（dev / prod）
- [ ] 前端打包 + Nginx 部署
- [ ] 上线验证
- [ ] 答辩 PPT + 演示彩排

## 4. 依赖清单

### 后端 Maven 依赖

```xml
<!-- 核心 -->
spring-boot-starter-web
spring-boot-starter-aop
spring-boot-starter-validation
spring-boot-starter-websocket

<!-- 数据库 -->
mybatis-plus-boot-starter (3.5.2)
mysql-connector-java

<!-- 缓存 -->
spring-boot-starter-data-redis

<!-- 搜索 -->
spring-boot-starter-data-elasticsearch

<!-- 消息队列 -->
rocketmq-spring-boot-starter (2.2.3)

<!-- 工具 -->
lombok
hutool-all (工具库)
jjwt (JWT)
jsoup (XSS 白名单过滤)
guava (限流 RateLimiter)
knife4j (Swagger 增强)
```

### 前端 npm 依赖

```json
{
  "dependencies": {
    "vue": "^3.x",
    "ant-design-vue": "^4.x",
    "axios": "^1.x",
    "vuex": "^4.x",
    "vue-router": "^4.x",
    "echarts": "^5.x",
    "@wangeditor/editor": "^5.x",
    "@wangeditor/editor-for-vue": "^5.x"
  },
  "devDependencies": {
    "typescript": "^5.x",
    "@vue/cli-service": "^5.x",
    "eslint": "^8.x"
  }
}
```

## 5. 环境变量

### 后端 application-dev.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ocean_knowledge?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
  redis:
    host: localhost
    port: 6379
  elasticsearch:
    uris: http://localhost:9200

jwt:
  secret: ${JWT_SECRET:ocean_knowledge_jwt_secret_2026}
  expiration: 86400000  # 24h

salt:
  password: ocean_knowledge_salt_2026
```

### Docker Compose 环境变量

```yaml
MYSQL_ROOT_PASSWORD: root123
MYSQL_DATABASE: ocean_knowledge
JWT_SECRET: ocean_knowledge_jwt_secret_2026
```

## 6. 最终结论

| 检查项 | 状态 |
|--------|------|
| 架构产物完整性 | ✅ 通过 |
| 实现条件满足度 | ✅ 通过 |
| 技术栈明确性 | ✅ 通过 |
| 安全设计完备性 | ✅ 通过 |
| 团队分工清晰度 | ✅ 通过 |

**✅ 实现准备度检查通过，可以开始写代码。**

## 7. 下一步行动

立即开始 **阶段 1：基础骨架**：

1. **A + B**：创建 Spring Boot 项目，配置依赖，实现 CommonResp + 全局异常
2. **C + D**：创建 Vue3 项目，配置 Ant Design Vue，实现登录页
3. **全员**：执行数据库初始化脚本

预计 Day 6 结束时，登录流程跑通，前后端骨架搭建完成。

---

## 8. V2 AI 功能实现路线图

### 阶段 5：AI 基础设施（V2 Day 1~3）

**后端**
- [ ] 添加 DeepSeek API 依赖（OkHttp / RestTemplate）
- [ ] 实现 AiProvider 接口 + DeepSeekProvider 实现
- [ ] 实现 AiRequest / AiResponse / ChatMessage 模型
- [ ] 实现 AI 配置（application.yml + 环境变量）
- [ ] 实现 AiConversation / AiMessage / AiUsageLog 实体和 Mapper
- [ ] 实现 Conversation Manager（会话创建、历史查询、上下文裁剪）
- [ ] 实现 Prompt Registry（系统 prompt 模板管理）
- [ ] 实现 Cost Meter（用量记录、费用计算）
- [ ] 实现 AI 接口限流（每用户每分钟/每天）

### 阶段 6：AI 问答 + 内容生成（V2 Day 4~7）

**后端**
- [ ] 实现 AiController：/api/ai/chat（多轮对话）
- [ ] 实现 AiController：/api/ai/generate（内容生成）
- [ ] 实现 AiController：/api/ai/conversations（会话列表）
- [ ] 实现 AiController：/api/ai/conversations/{id}/messages（消息历史）
- [ ] 实现 AiController：/api/ai/usage（用量统计）
- [ ] 实现 Output Validator（输出长度、格式校验）

**前端**
- [ ] 实现 AI 问答面板组件（AiChatPanel）
- [ ] 实现对话列表和消息历史展示
- [ ] 实现 AI 内容生成交互（生成/扩写/总结/润色）
- [ ] 对接 AI 接口

### 阶段 7：用户笔记（V2 Day 8~10）

**后端**
- [ ] 创建 Note 表（SQL）
- [ ] 实现 NoteController：笔记 CRUD + 公开笔记列表 + 点赞
- [ ] 实现笔记权限校验（仅编辑/删除自己的笔记）

**前端**
- [ ] 实现笔记编辑器（复用 wangEditor + AI 辅助按钮）
- [ ] 实现笔记列表页（我的笔记 + 公开笔记）
- [ ] 实现笔记详情页
- [ ] 对接笔记接口

### 阶段 8：测试和优化（V2 Day 11~14）

- [ ] AI 问答功能测试（多轮对话、上下文裁剪、超时处理）
- [ ] 内容生成功能测试（各场景生成质量）
- [ ] 笔记功能测试（CRUD、权限、公开/私有）
- [ ] AI 限流和用量统计验证
- [ ] 安全测试（API Key 保护、对话隔离、笔记权限）
- [ ] 性能测试（AI 响应时间、并发调用）
- [ ] Bug 修复和优化

# 海洋生物知识库 隐私/安全/风险设计

## 1. 文档目的

本文档定义系统的安全防护策略、隐私保护措施和风险控制方案。指导后端安全模块实现和答辩时的安全论述。

## 2. 数据分类

| 数据 | 敏感等级 | 说明 |
|------|---------|------|
| user.password | HIGH_SENSITIVE | 密码，加密存储，不返回前端，日志不打印 |
| JWT Token | SENSITIVE | 认证凭据，Redis 存储，过期自动失效 |
| user.login_name | NORMAL | 登录名，唯一标识 |
| user.name | NORMAL | 昵称 |
| ebook/doc/category | NORMAL | 知识内容，公开数据 |
| client IP | SENSITIVE | 用于点赞防重，不对外暴露 |
| DeepSeek API Key | HIGH_SENSITIVE | 环境变量存储，不返回前端，日志不打印 |
| AI 对话内容 | SENSITIVE | 用户的提问和 AI 回复，仅用户本人可见 |
| AI 用量数据 | NORMAL | token 用量和费用统计 |

## 3. 安全防护清单

### 3.1 XSS 防御

| 场景 | 措施 |
|------|------|
| 普通文本输入 | HTML 转义（`<` → `&lt;`，`>` → `&gt;`） |
| 富文本内容 | 白名单过滤，仅允许安全标签（p, h1-h6, img, a, ul, ol, li, table, tr, td, th, strong, em, br） |
| 响应输出 | Content-Type: application/json，浏览器不解析为 HTML |

**实现方式**：
- 自定义 XssFilter，拦截所有请求参数
- 富文本使用 Jsoup 白名单过滤
- 前端展示使用 v-text（非 v-html）渲染普通文本

### 3.2 SQL 注入防御

| 措施 | 说明 |
|------|------|
| MyBatis-Plus 参数化查询 | 所有查询使用 `#{}` 占位符，禁止 `${}` 拼接 |
| 输入校验 | JSR-303 注解校验参数类型和长度 |
| 最小权限 | 数据库账号仅授予必要权限 |

### 3.3 认证与授权

| 措施 | 说明 |
|------|------|
| JWT Token | 登录成功生成 Token，存入 Redis，TTL 24h |
| Token 校验 | 拦截器校验：Token 存在 + Redis 中有效 |
| 管理接口保护 | 管理类接口额外校验管理员权限 |
| 前端路由守卫 | 未登录不可访问管理页 |
| Token 刷新 | V1 不实现，过期需重新登录 |

### 3.4 接口限流

| 接口 | 限流策略 | 说明 |
|------|---------|------|
| POST /api/user/login | 每分钟 10 次/IP | 防暴力破解 |
| POST /api/doc/vote/{id} | 每分钟 30 次/IP | 防刷赞 |
| 其他接口 | 不限流 | V1 简化 |

**实现方式**：
- 自定义 @RateLimit 注解 + 拦截器
- 基于 Redis 计数器（滑动窗口）
- 超限返回 HTTP 429

### 3.5 密码安全

| 措施 | 说明 |
|------|------|
| 加密算法 | MD5 + 固定盐值（硬编码在配置文件） |
| 盐值示例 | `ocean_knowledge_salt_2026` |
| 复杂度要求 | 6-32 位，必须包含数字和字母 |
| 存储 | 加密后的 32 位字符串 |
| 传输 | HTTPS（生产环境）/ HTTP（本机演示） |

### 3.6 AI API 安全

| 措施 | 说明 |
|------|------|
| API Key 存储 | 环境变量 `DEEPSEEK_API_KEY`，不硬编码，不返回前端 |
| 日志脱敏 | AI 调用日志不打印 API Key |
| 接口限流 | AI 问答：每用户 10 次/分钟、100 次/天；内容生成：5 次/分钟 |
| 输出校验 | AI 输出经过长度检查和敏感词过滤 |
| 对话隔离 | 用户只能查看自己的对话历史，不能访问他人会话 |
| 笔记权限 | 私有笔记仅作者可见，公开笔记所有人可看 |
| 管理员确认 | AI 生成的文档内容不会自动保存，必须管理员手动确认 |
| 费用监控 | 记录每次调用的 token 用量和费用，支持按用户/功能统计 |

### 3.7 CORS 跨域

```java
// 允许的前端域名
allowedOrigins: ["http://localhost", "http://localhost:80"]
allowedMethods: ["GET", "POST", "DELETE", "OPTIONS"]
allowedHeaders: ["*"]
allowCredentials: true
```

### 3.7 日志安全

| 措施 | 说明 |
|------|------|
| MDC 流水号 | 每个请求生成唯一 LOG_ID |
| 敏感信息脱敏 | 日志中不打印 password、Token |
| 接口耗时 | AOP 记录请求参数、响应结果、耗时 |
| 慢查询告警 | 接口耗时 >500ms 打印 WARN |

## 4. 访问控制

### 4.1 公开接口（无需 Token）

```
GET  /api/ebook/list
GET  /api/category/list
GET  /api/doc/list
GET  /api/doc/{id}
GET  /api/snapshot/get-statistic
POST /api/user/login
```

### 4.2 登录用户接口（需 Token）

```
POST /api/doc/vote/{id}
GET  /api/user/logout
POST /api/ai/chat
POST /api/ai/generate
GET  /api/ai/conversations
GET  /api/ai/conversations/{id}/messages
GET  /api/ai/usage
GET  /api/note/list
POST /api/note/save
DELETE /api/note/delete/{id}
POST /api/note/vote/{id}
```

### 4.3 管理员接口（需 Token + 管理员身份）

```
POST   /api/ebook/save
DELETE /api/ebook/delete/{id}
POST   /api/category/save
DELETE /api/category/delete/{id}
POST   /api/doc/save
DELETE /api/doc/delete/{id}
GET    /api/user/list
POST   /api/user/save
DELETE /api/user/delete/{id}
POST   /api/user/reset-password
POST   /api/file/upload
```

## 5. 数据保护

### 5.1 密码保护

- 存储：MD5(明文 + 盐值)
- 传输：请求体 JSON，不明文出现在 URL
- 日志：过滤器脱敏，打印 `******`
- 响应：password 字段永远不返回

### 5.2 Token 保护

- 生成：JWT 签名使用服务端密钥
- 存储：Redis，Key 带 userId
- 传输：Header `token`，不在 URL 中传递
- 过期：24 小时自动失效

### 5.3 敏感字段过滤

后端响应中自动过滤：
```java
// User 实体序列化时排除 password
@JsonSerialize(using = SensitiveFieldSerializer.class)
private String password;
```

## 6. 性能要求

| 指标 | 要求 | 实现方式 |
|------|------|---------|
| 接口响应时间 | 95% 在 500ms 内 | Redis 缓存 + 分页查询 + 索引优化 |
| 并发支持 | 100 用户同时访问 | Spring Boot 默认线程池 + Redis 缓存热点数据 |
| 定时任务效率 | 快照生成 <5 秒 | 批量 INSERT + ON DUPLICATE KEY UPDATE |

## 7. 风险登记

| 风险 | 等级 | 影响 | 缓解措施 |
|------|------|------|---------|
| 密码泄露 | 高 | 账号被盗 | MD5+盐值加密，不返回前端，日志脱敏 |
| XSS 攻击 | 高 | 页面被注入恶意脚本 | 输入过滤 + 白名单 + Content-Type |
| SQL 注入 | 高 | 数据库被入侵 | MyBatis-Plus 参数化查询 |
| 暴力破解登录 | 中 | 账号被破解 | 接口限流（10 次/分钟） |
| 刷赞 | 中 | 数据失真 | Redis 防重 + 接口限流 |
| Token 被盗用 | 中 | 未授权访问 | Token 24h 过期 + Redis 双重校验 |
| 富文本存储型 XSS | 中 | 阅读页面被攻击 | Jsoup 白名单过滤 |
| 数据丢失 | 低 | 内容丢失 | Docker Volume 持久化（本机演示） |
| DDoS 攻击 | 低 | 服务不可用 | 本机演示环境，风险低 |
| AI API Key 泄露 | 高 | 他人盗用 Key 消费 | 环境变量存储，不返回前端，日志脱敏 |
| AI 调用费用超支 | 中 | 费用失控 | 接口限流 + 每日上限 + 用量日志 |
| AI 输出敏感内容 | 中 | 不当内容传播 | 输出校验 + 管理员确认机制 |
| AI 对话数据泄露 | 中 | 用户隐私泄露 | 对话隔离，仅本人可见 |

## 7. 安全测试检查清单

| 检查项 | 测试方法 | 预期结果 |
|--------|---------|---------|
| XSS 注入 | 输入 `<script>alert(1)</script>` | 被转义，不执行 |
| SQL 注入 | 输入 `' OR 1=1 --` | 参数化查询，不报错 |
| Token 过期 | 使用过期 Token 访问接口 | 返回 401 |
| 无 Token 访问 | 不带 Token 访问管理接口 | 返回 401 |
| 限流验证 | 连续 11 次调用登录接口 | 第 11 次返回 429 |
| 密码加密 | 数据库查看密码字段 | 非明文，32 位字符串 |
| 敏感字段 | 查看 API 响应 | 不包含 password 字段 |
| 路由守卫 | 未登录访问 /admin | 跳转首页 |

## 8. 未来扩展（V2/V3）

| 方向 | 说明 |
|------|------|
| HTTPS | 生产环境启用 SSL |
| RBAC 细粒度权限 | 角色+权限表，注解式鉴权 |
| Token 刷新机制 | 无感续期，避免频繁登录 |
| 接口全局限流 | 基于 Redis 的全局限流策略 |
| 审计日志 | 记录所有管理操作的变更历史 |
| 密码强度升级 | bcrypt 替代 MD5 |

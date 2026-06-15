# 海洋生物知识库 Data Model

## 1. 文档目的

本文档是 ORM/SQL 实现前的数据建模蓝图，定义所有实体、字段、关系和约束。后续数据库 Schema 和 API 契约均以本文档为依据。

## 2. 建模原则

| 原则 | 策略 |
|------|------|
| 主键策略 | 雪花算法生成 BIGINT（ebook、doc、category、user），content 使用自增 ID |
| 时间戳 | 所有实体包含 `create_time` 和 `update_time`，自动填充 |
| 租户隔离 | 无，单实例单租户 |
| 软删除 | V1 不使用，硬删除 |
| 审计 | view_count、vote_count 记录阅读和点赞统计 |
| 所有权 | 无复杂所有权模型，管理员拥有全部管理权 |
| 敏感字段 | user.password 为 HIGH_SENSITIVE |
| 版本管理 | V1 不使用 |
| 外部标识符 | 无 |

## 3. 核心实体概览

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  User    │     │ Category │     │  Ebook   │
│ 用户      │     │ 分类      │     │ 电子书    │
└────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │ parent         │
     │                └─→ self         │ ebook_id
     │                                 ▼
     │                            ┌──────────┐
     │                            │   Doc    │
     │                            │ 文档      │
     │                            └────┬─────┘
     │                                 │
     │                        ┌────────┼────────┐
     │                        │        │        │
     │                   ┌────▼───┐ ┌──▼─────┐ ┌▼──────────────┐
     │                   │Content │ │ parent │ │EbookSnapshot   │
     │                   │文档正文 │ │→ self  │ │数据快照         │
     │                   └────────┘ └────────┘ └───────────────┘
     │
     ├─────────────────────────────────────────────┐
     │                    │                         │
     ▼                    ▼                         ▼
┌──────────┐     ┌──────────────┐         ┌──────────────┐
│  Note    │     │AiConversation│         │AiUsageLog    │
│ 用户笔记  │     │AI 对话会话    │         │AI 用量日志    │
└──────────┘     └──────┬───────┘         └──────────────┘
                        │
                        ▼
                 ┌──────────────┐
                 │  AiMessage   │
                 │  AI 对话消息   │
                 └──────────────┘
```

## 4. 关系概览

| 关系类型 | 实体 A | 实体 B | 说明 |
|---------|--------|--------|------|
| 多对一 | Ebook | Category | 电子书关联两级分类（category1_id, category2_id） |
| 一对多 | Ebook | Doc | 一本电子书包含多篇文档 |
| 自引用 | Doc | Doc | 文档父子树形（parent_id） |
| 一对一 | Doc | Content | 文档与正文分离存储 |
| 一对多 | Ebook | EbookSnapshot | 每个电子书每天一条快照 |
| 一对多 | User | Note | 用户创建的笔记 |
| 一对多 | User | AiConversation | 用户的 AI 对话会话 |
| 一对多 | AiConversation | AiMessage | 会话中的消息 |
| 一对多 | User | AiUsageLog | 用户的 AI 用量记录 |

## 5. 枚举目录

本系统 V1 无显式枚举，所有对象无状态字段。未来扩展可考虑：

| 枚举 | 用途 | V1 状态 |
|------|------|---------|
| UserRole | 用户角色（admin/user） | 预留，V1 通过字段区分 |
| DocStatus | 文档状态（draft/published） | 不实现 |

## 6. 实体详情

### 6.1 User（用户）

**Purpose**：存储系统用户信息，支持登录认证和权限控制。

**Phase**：V1a

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| login_name | VARCHAR(50) | ✅ | 登录名，唯一 |
| name | VARCHAR(50) | ✅ | 昵称 |
| password | VARCHAR(32) | ✅ | MD5+盐值加密后的密码 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：无直接关联

**Sensitivity**：password → HIGH_SENSITIVE（不返回前端，日志不打印）

**Indexes / Constraints**：
- UNIQUE：login_name
- 无外键约束（应用层保证）

**Notes**：
- 密码加密：MD5(明文 + 固定盐值)，盐值硬编码在配置文件中
- 删除用户时不级联删除其他数据（用户无关联实体）

---

### 6.2 Category（分类）

**Purpose**：为电子书提供多级分类体系，支持无限级树形结构。

**Phase**：V1a

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| parent | BIGINT | ✅ | 父分类 ID，顶级分类为 0 |
| name | VARCHAR(20) | ✅ | 分类名称 |
| sort | INT | ✅ | 排序值，升序排列 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 自引用：parent → Category.id（树形结构）
- 被引用：Ebook.category1_id, Ebook.category2_id

**Indexes / Constraints**：
- INDEX：parent（查询子分类）
- 无唯一约束（同级分类允许重名，但不推荐）

**Notes**：
- 删除分类时需检查是否存在子分类，存在则禁止删除
- 不能将自己设为父分类（应用层校验）
- 电子书只挂到二级分类（category1_id, category2_id）

---

### 6.3 Ebook（电子书）

**Purpose**：知识库的顶层组织单元，包含元数据和统计信息。

**Phase**：V1a

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| name | VARCHAR(50) | ✅ | 电子书名称 |
| category1_id | BIGINT | ✅ | 一级分类 ID |
| category2_id | BIGINT | ✅ | 二级分类 ID |
| description | VARCHAR(200) | ❌ | 描述 |
| cover | VARCHAR(200) | ❌ | 封面图片路径 |
| doc_count | INT | default 0 | 文档总数 |
| view_count | INT | default 0 | 总阅读数 |
| vote_count | INT | default 0 | 总点赞数 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 多对一：category1_id → Category.id
- 多对一：category2_id → Category.id
- 一对多：Ebook.id → Doc.ebook_id

**Indexes / Constraints**：
- INDEX：category1_id, category2_id（分类筛选）
- INDEX：name（模糊搜索）

**Notes**：
- doc_count 由文档增删时同步更新（应用层维护）
- view_count、vote_count 由快照任务汇总更新

---

### 6.4 Doc（文档）

**Purpose**：电子书下的具体内容单元，支持无限级树形结构。

**Phase**：V1a

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| ebook_id | BIGINT | ✅ | 所属电子书 ID |
| parent | BIGINT | ✅ | 父文档 ID，顶级文档为 0 |
| name | VARCHAR(50) | ✅ | 文档名称 |
| sort | INT | ✅ | 排序值 |
| view_count | INT | default 0 | 阅读数 |
| vote_count | INT | default 0 | 点赞数 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 多对一：ebook_id → Ebook.id
- 自引用：parent → Doc.id（树形结构）
- 一对一：Doc.id → Content.id

**Indexes / Constraints**：
- INDEX：ebook_id（查询某电子书下的文档）
- INDEX：parent（查询子文档）

**Notes**：
- 删除文档时递归删除所有子文档及对应的 Content 记录
- view_count 在用户阅读时 +1
- vote_count 在用户点赞时 +1

---

### 6.5 Content（文档正文）

**Purpose**：存储文档的富文本 HTML 内容，与 Doc 分离以优化查询性能。

**Phase**：V1a

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 与 Doc.id 一致 |
| content | MEDIUMTEXT | ❌ | 富文本 HTML 内容 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 一对一：Content.id → Doc.id

**Indexes / Constraints**：
- PK：id（同时也是 Doc 的外键）

**Notes**：
- 使用 MEDIUMTEXT（最大 16MB），单篇文档限制 1MB
- 查询文档列表时不 JOIN 此表，仅在阅读时查询
- 内容需经过 XSS 白名单过滤后存储

---

### 6.6 EbookSnapshot（电子书数据快照）

**Purpose**：记录电子书每日的阅读和点赞统计，用于趋势分析。

**Phase**：V1b

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 自增 |
| ebook_id | BIGINT | ✅ | 电子书 ID |
| date | DATE | ✅ | 快照日期 |
| view_count | INT | default 0 | 当日总阅读数 |
| vote_count | INT | default 0 | 当日总点赞数 |
| view_increase | INT | default 0 | 当日阅读增量 |
| vote_increase | INT | default 0 | 当日点赞增量 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 多对一：ebook_id → Ebook.id

**Indexes / Constraints**：
- UNIQUE：ebook_id + date（同一天同一电子书只有一条记录）
- INDEX：date（按日期查询）

**Notes**：
- 定时任务每分钟执行，使用 INSERT ... ON DUPLICATE KEY UPDATE 保证幂等
- view_increase = 当前 view_count - 昨日 view_count
- vote_increase = 当前 vote_count - 昨日 vote_count
- 建议保留 30 天，超期清理

---

### 6.7 Note（用户笔记）

**Purpose**：用户创建的学习笔记，支持 AI 辅助生成，可公开分享。

**Phase**：V2

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| user_id | BIGINT | ✅ | 作者 ID |
| title | VARCHAR(100) | ✅ | 笔记标题 |
| content | MEDIUMTEXT | ❌ | 富文本 HTML 内容 |
| is_public | TINYINT | ✅ | 是否公开（0 私有 1 公有），默认 0 |
| view_count | INT | default 0 | 阅读数 |
| vote_count | INT | default 0 | 点赞数 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 多对一：user_id → User.id

**Indexes / Constraints**：
- INDEX：user_id（查询用户的笔记）
- INDEX：is_public（查询公开笔记）

**Notes**：
- 内容需经过 XSS 白名单过滤后存储
- 公开笔记所有用户可见，私有笔记仅作者可见

---

### 6.8 AiConversation（AI 对话会话）

**Purpose**：记录用户的 AI 对话会话，支持多轮对话。

**Phase**：V2

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | VARCHAR(32) | PK | UUID |
| user_id | BIGINT | ✅ | 用户 ID |
| title | VARCHAR(100) | ❌ | 会话标题（首条消息截取前 50 字符） |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

**Relationships**：
- 多对一：user_id → User.id
- 一对多：AiConversation.id → AiMessage.conversation_id

**Indexes / Constraints**：
- INDEX：user_id（查询用户的会话列表）

**Notes**：
- 会话保留最近 30 天，超期清理

---

### 6.9 AiMessage（AI 对话消息）

**Purpose**：存储 AI 对话中的每条消息（用户输入和 AI 回复）。

**Phase**：V2

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | VARCHAR(32) | PK | UUID |
| conversation_id | VARCHAR(32) | ✅ | 会话 ID |
| role | VARCHAR(10) | ✅ | 消息角色：user / assistant |
| content | TEXT | ✅ | 消息内容 |
| prompt_tokens | INT | default 0 | 输入 token 数（仅 assistant 消息） |
| completion_tokens | INT | default 0 | 输出 token 数（仅 assistant 消息） |
| create_time | DATETIME | auto | 创建时间 |

**Relationships**：
- 多对一：conversation_id → AiConversation.id

**Indexes / Constraints**：
- INDEX：conversation_id（查询会话的消息列表）

**Notes**：
- 用户消息的 token 字段为 0
- 最多保留每会话最近 100 条消息

---

### 6.10 AiUsageLog（AI 用量日志）

**Purpose**：记录每次 AI 调用的用量和费用，用于成本统计和监控。

**Phase**：V2

**Key Fields**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 自增 |
| user_id | BIGINT | ✅ | 用户 ID |
| feature | VARCHAR(20) | ✅ | 功能类型：chat / note / doc_assist |
| provider | VARCHAR(20) | ✅ | 供应商名称（deepseek / tongyi 等） |
| model | VARCHAR(50) | ✅ | 模型名称 |
| prompt_tokens | INT | ✅ | 输入 token 数 |
| completion_tokens | INT | ✅ | 输出 token 数 |
| total_tokens | INT | ✅ | 总 token 数 |
| cost_yuan | DECIMAL(10,6) | ✅ | 费用（元） |
| latency_ms | INT | ✅ | 响应耗时（毫秒） |
| status | VARCHAR(10) | ✅ | 调用状态：success / error |
| create_time | DATETIME | auto | 创建时间 |

**Relationships**：
- 多对一：user_id → User.id

**Indexes / Constraints**：
- INDEX：user_id（按用户统计用量）
- INDEX：create_time（按时间统计）

**Notes**：
- 失败的调用也记录（status=error, cost_yuan=0）
- 保留 90 天，超期清理

---

## 7. 数据保留和删除

| 实体 | 删除策略 | 保留规则 |
|------|---------|---------|
| User | 硬删除 | 无保留 |
| Category | 硬删除（有子分类时禁止） | 无保留 |
| Ebook | 硬删除 | 无保留 |
| Doc | 硬删除（含递归子文档） | 无保留 |
| Content | 随 Doc 级联删除 | 无保留 |
| EbookSnapshot | 定时清理 | 保留 30 天 |
| Note | 硬删除 | 无保留 |
| AiConversation | 定时清理 | 保留 30 天 |
| AiMessage | 随会话级联删除 | 保留 30 天 |
| AiUsageLog | 定时清理 | 保留 90 天 |

## 8. V1 必须模型（实现优先级）

| 优先级 | 实体 | 阶段 | 说明 |
|--------|------|------|------|
| P1 | User | V1a | 登录认证基础 |
| P1 | Category | V1a | 分类体系基础 |
| P1 | Ebook | V1a | 知识库主体 |
| P1 | Doc | V1a | 文档内容 |
| P1 | Content | V1a | 文档正文 |
| P2 | EbookSnapshot | V1b | 数据统计 |

## 9. V2 新增模型（AI + 笔记）

| 实体 | 用途 | 预计版本 |
|------|------|---------|
| Note | 用户学习笔记 | V2 |
| AiConversation | AI 对话会话 | V2 |
| AiMessage | AI 对话消息 | V2 |
| AiUsageLog | AI 用量日志 | V2 |

## 10. 预留模型（V2/V3）

| 实体 | 用途 | 预计版本 |
|------|------|---------|
| Comment | 文档评论 | V2 |
| UserFavorite | 用户收藏 | V2 |
| ReadHistory | 阅读历史 | V2 |
| Role | 角色（RBAC） | V2 |
| Permission | 权限 | V2 |

## 10. 开放问题

| 问题 | 影响 | 建议 |
|------|------|------|
| EbookSnapshot 的 id 策略 | 性能 | 使用自增 ID（非雪花），因为是内部统计表 |
| Content 的字符集 | 存储 | 使用 utf8mb4，支持 emoji 和特殊字符 |
| 是否需要 ebook 表冗余 category 名称 | 查询性能 | 不冗余，通过 JOIN 查询（数据量小） |

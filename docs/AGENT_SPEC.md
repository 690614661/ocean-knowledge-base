# 海洋生物知识库 AI / 自动化规格

## 1. 文档目的

本文档定义系统新增 AI 功能的架构设计、能力边界、供应商抽象、上下文管理、成本计量和审计要求。后续 AI 模块实现以本文档为依据。

## 2. 能力分类

| 能力 | 类型 | 说明 |
|------|------|------|
| AI 问答 | 官方功能 | 用户提问海洋生物相关问题，AI 基于外部知识回答 |
| AI 内容生成（用户笔记） | 官方功能 | 用户使用 AI 辅助生成学习笔记，可公开分享 |
| AI 内容生成（文档辅助） | 官方功能 | 管理员使用 AI 辅助撰写/扩展文档内容 |
| 多轮对话 | 官方功能 | 问答支持上下文连续对话 |
| 供应商切换 | 内部机制 | 抽象层支持切换不同 AI 供应商（当前 DeepSeek） |
| 禁止动作 | - | AI 不能直接修改/删除系统数据，不能绕过人工审核 |

## 3. 编排架构

```
┌─────────────────────────────────────────────────────┐
│                    前端 (Vue3)                        │
│  ┌──────────────┐  ┌──────────────┐                  │
│  │  AI 问答面板  │  │ 笔记编辑器   │                  │
│  │  (多轮对话)   │  │ (AI 辅助)    │                  │
│  └──────┬───────┘  └──────┬───────┘                  │
└─────────┼─────────────────┼──────────────────────────┘
          │ REST API        │ REST API
┌─────────▼─────────────────▼──────────────────────────┐
│              Spring Boot 后端                         │
│  ┌────────────────────────────────────┐               │
│  │         AI Controller              │               │
│  └──────────────┬─────────────────────┘               │
│                 ▼                                      │
│  ┌────────────────────────────────────┐               │
│  │         AI Service (编排层)         │               │
│  │  ┌──────────┐  ┌───────────────┐   │               │
│  │  │ Context  │  │ Conversation  │   │               │
│  │  │ Builder  │  │ Manager       │   │               │
│  │  └──────────┘  └───────────────┘   │               │
│  │  ┌──────────┐  ┌───────────────┐   │               │
│  │  │ Prompt   │  │ Output        │   │               │
│  │  │ Registry │  │ Validator     │   │               │
│  │  └──────────┘  └───────────────┘   │               │
│  │  ┌──────────┐  ┌───────────────┐   │               │
│  │  │ Cost     │  │ Audit         │   │               │
│  │  │ Meter    │  │ Logger        │   │               │
│  │  └──────────┘  └───────────────┘   │               │
│  └──────────────┬─────────────────────┘               │
│                 ▼                                      │
│  ┌────────────────────────────────────┐               │
│  │     AI Provider (供应商抽象层)      │               │
│  │  ┌──────────────┐                  │               │
│  │  │ AiProvider   │ (interface)      │               │
│  │  └──────┬───────┘                  │               │
│  │         ▼                          │               │
│  │  ┌──────────────┐ ┌─────────────┐  │               │
│  │  │ DeepSeek     │ │ 通义千问     │  │               │
│  │  │ Provider     │ │ Provider     │  │               │
│  │  └──────────────┘ └─────────────┘  │               │
│  └────────────────────────────────────┘               │
└──────────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
   DeepSeek API          通义千问 API
   (外部)                (外部)
```

### 核心组件

| 组件 | 职责 |
|------|------|
| AI Controller | 接收前端请求，参数校验，权限检查 |
| AI Service | 编排层：组装上下文、调用供应商、记录用量、返回结果 |
| Context Builder | 构建 AI 请求上下文（系统 prompt + 对话历史 + 用户输入） |
| Conversation Manager | 管理多轮对话的会话创建、历史存储、上下文裁剪 |
| Prompt Registry | 管理系统 prompt 模板（问答模板、笔记生成模板、文档辅助模板） |
| Output Validator | 校验 AI 输出（长度、格式、敏感词过滤） |
| Cost Meter | 记录每次调用的 token 用量和费用 |
| Audit Logger | 记录 AI 调用日志（用户、时间、输入、输出、用量） |
| AiProvider | 供应商抽象接口，统一请求/响应格式 |

## 4. 供应商抽象设计

### 4.1 AiProvider 接口

```java
public interface AiProvider {
    /**
     * 同步调用 AI
     * @param request 统一请求格式
     * @return 统一响应格式
     */
    AiResponse chat(AiRequest request);

    /**
     * 获取供应商名称
     */
    String getName();
}
```

### 4.2 统一请求格式

```java
public class AiRequest {
    private String systemPrompt;           // 系统角色设定
    private List<ChatMessage> messages;    // 对话历史
    private String model;                  // 模型名称（可选，用默认）
    private Double temperature;            // 温度参数（可选）
    private Integer maxTokens;             // 最大输出 token（可选）
}

public class ChatMessage {
    private String role;    // "system" / "user" / "assistant"
    private String content; // 消息内容
}
```

### 4.3 统一响应格式

```java
public class AiResponse {
    private String content;              // AI 回复内容
    private Integer promptTokens;        // 输入 token 数
    private Integer completionTokens;    // 输出 token 数
    private Integer totalTokens;         // 总 token 数
    private String model;                // 实际使用的模型
    private String provider;             // 供应商名称
    private Long latencyMs;              // 响应耗时（毫秒）
}
```

### 4.4 供应商配置

```yaml
ai:
  provider: deepseek                    # 当前使用的供应商
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: https://api.deepseek.com
    model: deepseek-chat
    max-tokens: 4096
    temperature: 0.7
    timeout: 60s
  # 未来扩展
  # tongyi:
  #   api-key: ${TONGYI_API_KEY}
  #   base-url: https://dashscope.aliyuncs.com
  #   model: qwen-turbo
```

## 5. 功能规格

### 5.1 AI 问答

**触发方式**：用户在问答面板输入问题并发送

**上下文构建**：
```
[系统 Prompt] 你是海洋生物知识库的 AI 助手，擅长解答海洋生物相关问题...
[对话历史]    user: 鲸鱼是鱼吗？  assistant: 不是，鲸鱼是哺乳动物...
[用户输入]    那鲸鱼为什么能在水里生活？
```

**上下文窗口管理**：
- 最多保留最近 20 轮对话
- 当超过 token 限制时，从最早的对话开始裁剪
- 系统 prompt 始终保留

**输出处理**：
- Markdown 格式返回（前端渲染）
- 最大输出 4096 tokens
- 超时 60 秒

### 5.2 AI 内容生成 — 用户笔记

**触发方式**：笔记编辑器中点击"AI 辅助"按钮

**场景**：
| 场景 | Prompt 策略 |
|------|------------|
| 生成笔记 | 用户输入主题 → AI 生成结构化笔记 |
| 扩写内容 | 用户选中段落 → AI 扩展补充 |
| 总结提炼 | 用户选中大段内容 → AI 生成摘要 |
| 修改润色 | 用户选中内容 → AI 优化表达 |

**笔记实体**：
- 用户创建，可公开分享
- 支持富文本（与文档编辑器共用 wangEditor）
- 与 AI 问答独立，不关联对话历史

### 5.3 AI 内容生成 — 管理员文档辅助

**触发方式**：文档编辑器中点击"AI 辅助"按钮

**场景**：
| 场景 | 说明 |
|------|------|
| 生成文档大纲 | 输入主题 → AI 生成章节结构 |
| 扩展章节 | 选中章节标题 → AI 生成该章节内容 |
| 补充内容 | 选中段落 → AI 补充更多细节 |
| 优化表达 | 选中内容 → AI 润色优化 |

**约束**：
- AI 生成的内容**不会**自动保存，必须由管理员确认后才写入数据库
- 管理员可编辑 AI 生成的内容后再保存

## 6. 通用输入 Envelope

```json
{
  "conversationId": "conv_123456",       // 问答会话 ID（新建时为空）
  "message": "鲸鱼为什么能在水里生活？", // 用户输入
  "context": {                           // 可选：内容生成时的上下文
    "type": "note_generate",             // note_generate | note_expand | doc_assist
    "topic": "深海生态笔记",             // 生成主题
    "selectedText": "",                  // 选中的文本（扩写/润色时）
    "docId": null                        // 文档辅助时的文档 ID
  }
}
```

## 7. 通用输出 Envelope

```json
{
  "success": true,
  "message": "操作成功",
  "content": {
    "conversationId": "conv_123456",     // 会话 ID
    "messageId": "msg_789",             // 消息 ID
    "role": "assistant",
    "content": "鲸鱼能在水里生活是因为...",  // AI 回复
    "usage": {
      "promptTokens": 520,
      "completionTokens": 380,
      "totalTokens": 900,
      "estimatedCost": "0.0018"          // 预估费用（元）
    }
  }
}
```

## 8. 数据模型新增

### 8.1 AiConversation（AI 对话会话）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | VARCHAR(32) | PK | UUID |
| user_id | BIGINT | ✅ | 用户 ID |
| title | VARCHAR(100) | ❌ | 会话标题（首条消息截取） |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

### 8.2 AiMessage（AI 对话消息）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | VARCHAR(32) | PK | UUID |
| conversation_id | VARCHAR(32) | ✅ | 会话 ID |
| role | VARCHAR(10) | ✅ | user / assistant |
| content | TEXT | ✅ | 消息内容 |
| prompt_tokens | INT | default 0 | 输入 token 数 |
| completion_tokens | INT | default 0 | 输出 token 数 |
| create_time | DATETIME | auto | 创建时间 |

### 8.3 Note（用户笔记）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 雪花算法生成 |
| user_id | BIGINT | ✅ | 作者 ID |
| title | VARCHAR(100) | ✅ | 笔记标题 |
| content | MEDIUMTEXT | ❌ | 富文本 HTML 内容 |
| is_public | TINYINT | default 0 | 是否公开（0 私有 1 公开） |
| view_count | INT | default 0 | 阅读数 |
| vote_count | INT | default 0 | 点赞数 |
| create_time | DATETIME | auto | 创建时间 |
| update_time | DATETIME | auto | 更新时间 |

### 8.4 AiUsageLog（AI 用量日志）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 自增 |
| user_id | BIGINT | ✅ | 用户 ID |
| feature | VARCHAR(20) | ✅ | 功能类型：chat / note / doc_assist |
| provider | VARCHAR(20) | ✅ | 供应商名称 |
| model | VARCHAR(50) | ✅ | 模型名称 |
| prompt_tokens | INT | ✅ | 输入 token 数 |
| completion_tokens | INT | ✅ | 输出 token 数 |
| total_tokens | INT | ✅ | 总 token 数 |
| cost_yuan | DECIMAL(10,6) | ✅ | 费用（元） |
| latency_ms | INT | ✅ | 响应耗时 |
| create_time | DATETIME | auto | 创建时间 |

## 9. Prompt 模板

### 9.1 通用问答 System Prompt

```
你是"海洋知识库"的 AI 助手，专注于海洋生物领域。
你的职责：
1. 解答用户关于海洋生物的问题（分类、生态、习性、保护等）
2. 回答应准确、简洁、有条理
3. 使用 Markdown 格式输出，适当使用标题、列表、加粗
4. 如果问题超出海洋生物范围，礼貌地引导回主题
5. 不确定的信息要明确标注

注意：你的知识来自训练数据，不基于知识库内文档检索。
```

### 9.2 笔记生成 System Prompt

```
你是一个学习笔记生成助手，帮助用户整理海洋生物知识。
根据用户提供的主题，生成结构清晰、内容丰富的学习笔记。
要求：
1. 使用 Markdown 格式
2. 包含标题、要点、关键概念
3. 适当使用列表和层级结构
4. 内容准确、通俗易懂
```

### 9.3 文档辅助 System Prompt

```
你是一个文档写作助手，帮助管理员撰写海洋生物知识库的文档内容。
根据用户的需求（生成大纲、扩展内容、补充细节、优化表达），
输出高质量的文档内容。
要求：
1. 使用 HTML 富文本格式（与知识库文档格式一致）
2. 内容专业、准确、结构清晰
3. 适合在线阅读，段落不宜过长
```

## 10. 成本计量

### 10.1 DeepSeek 定价参考

| 模型 | 输入价格 | 输出价格 |
|------|---------|---------|
| deepseek-chat | ¥1 / 1M tokens | ¥2 / 1M tokens |

### 10.2 成本控制

| 措施 | 说明 |
|------|------|
| 单次最大 token | 4096 tokens（输出） |
| 对话历史裁剪 | 最多 20 轮，超限裁剪早期消息 |
| 接口限流 | 每用户每分钟 10 次 AI 调用 |
| 每日上限 | 每用户每天 100 次 AI 调用 |
| 费用记录 | 每次调用记录到 ai_usage_log |

## 11. 人工审核

| 场景 | 审核要求 |
|------|---------|
| AI 问答 | 无需审核，直接返回用户 |
| 用户笔记 | 用户自行编辑后保存，公开笔记所有人可见 |
| 管理员文档辅助 | AI 生成内容不会自动保存，管理员必须手动确认 |

## 12. 失败处理

| 失败类型 | 处理方式 |
|---------|---------|
| API 超时 | 返回"AI 响应超时，请稍后重试"，记录日志 |
| API 返回错误 | 返回"AI 服务暂时不可用"，记录错误详情 |
| Token 超限 | 自动裁剪对话历史后重试一次 |
| 余额不足 | 返回"AI 服务暂不可用"，记录告警日志 |
| 网络异常 | 返回"网络异常，请稍后重试" |

**失败时仍然记录用量日志**（feature 标记为 error，cost 为 0）。

## 13. 禁止动作

- AI 不能直接写入或修改数据库（ebook、doc、user 等）
- AI 不能执行删除操作
- AI 不能访问其他用户的对话历史或私有笔记
- AI 不能绕过接口限流
- AI 生成的内容不能自动发布（必须人工确认）
- AI 不能访问用户的密码、Token 等敏感信息

## 14. 接口限流（AI 专用）

| 接口 | 限流策略 | 说明 |
|------|---------|------|
| POST /api/ai/chat | 每分钟 10 次/用户 | 防滥用 |
| POST /api/ai/chat | 每天 100 次/用户 | 成本控制 |
| POST /api/ai/generate | 每分钟 5 次/用户 | 内容生成限流更严格 |

## 15. 开放问题

| 问题 | 影响 | 建议 |
|------|------|------|
| 对话历史存储策略 | 数据量增长 | 保留最近 30 天，超期清理 |
| AI 生成内容是否需要标记 | 用户体验 | 笔记中标注"AI 辅助生成"，可选 |
| 是否支持流式输出（SSE） | 用户体验 | V2 实现，V1 使用同步请求 |
| 笔记是否支持导出 | 功能完整性 | V2 考虑，V1 仅在线编辑和查看 |

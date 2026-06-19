package com.ocean.ai.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.ai.model.AiRequest;
import com.ocean.ai.model.AiResponse;
import com.ocean.ai.model.ChatMessage;
import com.ocean.ai.provider.AiProvider;
import com.ocean.common.BusinessException;
import com.ocean.domain.AiConversation;
import com.ocean.domain.AiMessage;
import com.ocean.domain.AiUsageLog;
import com.ocean.mapper.AiConversationMapper;
import com.ocean.mapper.AiMessageMapper;
import com.ocean.mapper.AiUsageLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiService {

    @Autowired
    private AiProvider aiProvider;

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiMessageMapper messageMapper;

    @Autowired
    private AiUsageLogMapper usageLogMapper;

    private static final String CHAT_SYSTEM_PROMPT = "你是\"海洋知识库\"的 AI 助手，专注于海洋生物领域。\n" +
            "你的职责：\n" +
            "1. 解答用户关于海洋生物的问题（分类、生态、习性、保护等）\n" +
            "2. 回答应准确、简洁、有条理\n" +
            "3. 使用 Markdown 格式输出，适当使用标题、列表、加粗\n" +
            "4. 如果问题超出海洋生物范围，礼貌地引导回主题\n" +
            "5. 不确定的信息要明确标注\n\n" +
            "注意：你的知识来自训练数据，不基于知识库内文档检索。";

    private static final String NOTE_GENERATE_PROMPT = "你是一个学习笔记生成助手，帮助用户整理海洋生物知识。\n" +
            "根据用户提供的主题，生成结构清晰、内容丰富的学习笔记。\n" +
            "要求：\n1. 使用 Markdown 格式\n2. 包含标题、要点、关键概念\n3. 适当使用列表和层级结构\n4. 内容准确、通俗易懂";

    private static final String DOC_ASSIST_PROMPT = "你是一个文档写作助手，帮助管理员撰写海洋生物知识库的文档内容。\n" +
            "根据用户的需求（生成大纲、扩展内容、补充细节、优化表达），\n" +
            "输出高质量的文档内容。\n" +
            "要求：\n1. 使用 HTML 富文本格式\n2. 内容专业、准确、结构清晰\n3. 适合在线阅读，段落不宜过长";

    private static final int MAX_HISTORY_ROUNDS = 20;

    public SseEmitter streamChat(String conversationId, String message, Long userId) {
        // 创建或获取会话
        AiConversation conversation;
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = IdUtil.simpleUUID();
            conversation = new AiConversation();
            conversation.setId(conversationId);
            conversation.setUserId(userId);
            conversation.setTitle(message.length() > 50 ? message.substring(0, 50) : message);
            conversationMapper.insert(conversation);
        } else {
            conversation = conversationMapper.selectById(conversationId);
            if (conversation == null || !conversation.getUserId().equals(userId)) {
                throw new BusinessException("会话不存在");
            }
        }

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setId(IdUtil.simpleUUID());
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        messageMapper.insert(userMsg);

        // 构建上下文
        List<AiMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByDesc(AiMessage::getCreateTime)
                        .last("LIMIT " + (MAX_HISTORY_ROUNDS * 2)));
        Collections.reverse(history);

        List<ChatMessage> messages = new ArrayList<>();
        for (AiMessage msg : history) {
            messages.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }

        AiRequest request = new AiRequest();
        request.setSystemPrompt(CHAT_SYSTEM_PROMPT);
        request.setMessages(messages);

        String finalConversationId = conversationId;
        long startTime = System.currentTimeMillis();
        StringBuilder contentAccumulator = new StringBuilder();

        SseEmitter emitter = new SseEmitter(120000L);

        aiProvider.streamChat(
            request,
            // onChunk: 每段内容
            chunk -> {
                contentAccumulator.append(JSON.parseObject(chunk).getString("content"));
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(chunk));
                } catch (Exception e) {
                    throw new RuntimeException("SSE发送失败", e);
                }
            },
            // onUsage: 用量信息（可选）
            usage -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("usage")
                        .data(usage));
                } catch (Exception e) {
                    log.warn("SSE发送用量信息失败", e);
                }
            },
            // onComplete: 完成
            () -> {
                try {
                    // 保存AI消息
                    String fullContent = contentAccumulator.toString();
                    AiMessage assistantMsg = new AiMessage();
                    assistantMsg.setId(IdUtil.simpleUUID());
                    assistantMsg.setConversationId(finalConversationId);
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(fullContent);
                    messageMapper.insert(assistantMsg);

                    // 记录用量（从流式响应中无法获取精确token数，用估算值）
                    int estimatedTokens = (int) Math.ceil(fullContent.length() / 2.0);
                    BigDecimal cost = calculateCost(0, estimatedTokens);
                    long latency = System.currentTimeMillis() - startTime;
                    logUsage(userId, "chat", 0, estimatedTokens, estimatedTokens, cost, latency, "success");

                    // 发送完成事件
                    JSONObject doneData = new JSONObject();
                    doneData.put("conversationId", finalConversationId);
                    emitter.send(SseEmitter.event()
                        .name("done")
                        .data(doneData.toJSONString()));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("SSE完成回调处理失败", e);
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ignored) {}
                }
            },
            // onError: 错误
            error -> {
                log.error("SSE流式对话出错", error);
                try {
                    JSONObject errorData = new JSONObject();
                    errorData.put("message", "AI响应中断，请重试");
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(errorData.toJSONString()));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(error);
                }
            }
        );

        return emitter;
    }

    public Map<String, Object> chat(String conversationId, String message, Long userId) {
        // 创建或获取会话
        AiConversation conversation;
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = IdUtil.simpleUUID();
            conversation = new AiConversation();
            conversation.setId(conversationId);
            conversation.setUserId(userId);
            conversation.setTitle(message.length() > 50 ? message.substring(0, 50) : message);
            conversationMapper.insert(conversation);
        } else {
            conversation = conversationMapper.selectById(conversationId);
            if (conversation == null || !conversation.getUserId().equals(userId)) {
                throw new BusinessException("会话不存在");
            }
        }

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setId(IdUtil.simpleUUID());
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        messageMapper.insert(userMsg);

        // 构建上下文
        List<AiMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByDesc(AiMessage::getCreateTime)
                        .last("LIMIT " + (MAX_HISTORY_ROUNDS * 2)));
        Collections.reverse(history);

        List<ChatMessage> messages = new ArrayList<>();
        for (AiMessage msg : history) {
            messages.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }

        // 调用 AI
        AiRequest request = new AiRequest();
        request.setSystemPrompt(CHAT_SYSTEM_PROMPT);
        request.setMessages(messages);

        AiResponse response;
        try {
            response = aiProvider.chat(request);
        } catch (Exception e) {
            // 记录失败日志
            logUsage(userId, "chat", 0, 0, 0, BigDecimal.ZERO, 0L, "error");
            throw new BusinessException(e.getMessage());
        }

        // 保存 AI 消息
        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setId(IdUtil.simpleUUID());
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(response.getContent());
        assistantMsg.setPromptTokens(response.getPromptTokens());
        assistantMsg.setCompletionTokens(response.getCompletionTokens());
        messageMapper.insert(assistantMsg);

        // 记录用量
        BigDecimal cost = calculateCost(response.getPromptTokens(), response.getCompletionTokens());
        logUsage(userId, "chat", response.getPromptTokens(), response.getCompletionTokens(),
                response.getTotalTokens(), cost, response.getLatencyMs(), "success");

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("messageId", assistantMsg.getId());
        result.put("role", "assistant");
        result.put("content", response.getContent());
        Map<String, Object> usage = new HashMap<>();
        usage.put("promptTokens", response.getPromptTokens());
        usage.put("completionTokens", response.getCompletionTokens());
        usage.put("totalTokens", response.getTotalTokens());
        usage.put("estimatedCost", cost.toPlainString());
        result.put("usage", usage);

        return result;
    }

    public Map<String, Object> generate(String type, String topic, String selectedText, Long userId) {
        String systemPrompt;
        String userMessage;

        switch (type) {
            case "note_generate":
                systemPrompt = NOTE_GENERATE_PROMPT;
                userMessage = "请帮我生成关于\"" + topic + "\"的学习笔记";
                break;
            case "note_expand":
                systemPrompt = NOTE_GENERATE_PROMPT;
                userMessage = "请帮我扩写以下内容：\n" + selectedText;
                break;
            case "note_summarize":
                systemPrompt = NOTE_GENERATE_PROMPT;
                userMessage = "请帮我总结以下内容：\n" + selectedText;
                break;
            case "note_polish":
                systemPrompt = NOTE_GENERATE_PROMPT;
                userMessage = "请帮我润色以下内容：\n" + selectedText;
                break;
            case "doc_outline":
                systemPrompt = DOC_ASSIST_PROMPT;
                userMessage = "请帮我生成关于\"" + topic + "\"的文档大纲";
                break;
            case "doc_expand":
                systemPrompt = DOC_ASSIST_PROMPT;
                userMessage = "请帮我扩展以下章节内容：\n" + selectedText;
                break;
            case "doc_supplement":
                systemPrompt = DOC_ASSIST_PROMPT;
                userMessage = "请帮我补充以下内容的细节：\n" + selectedText;
                break;
            case "doc_polish":
                systemPrompt = DOC_ASSIST_PROMPT;
                userMessage = "请帮我润色优化以下内容：\n" + selectedText;
                break;
            default:
                throw new BusinessException("不支持的生成类型");
        }

        String feature = type.startsWith("doc_") ? "doc_assist" : "note";

        AiRequest request = new AiRequest();
        request.setSystemPrompt(systemPrompt);
        request.setMessages(Collections.singletonList(new ChatMessage("user", userMessage)));

        AiResponse response;
        try {
            response = aiProvider.chat(request);
        } catch (Exception e) {
            logUsage(userId, feature, 0, 0, 0, BigDecimal.ZERO, 0L, "error");
            throw new BusinessException(e.getMessage());
        }

        BigDecimal cost = calculateCost(response.getPromptTokens(), response.getCompletionTokens());
        logUsage(userId, feature, response.getPromptTokens(), response.getCompletionTokens(),
                response.getTotalTokens(), cost, response.getLatencyMs(), "success");

        Map<String, Object> result = new HashMap<>();
        result.put("text", response.getContent());
        Map<String, Object> usage = new HashMap<>();
        usage.put("promptTokens", response.getPromptTokens());
        usage.put("completionTokens", response.getCompletionTokens());
        usage.put("totalTokens", response.getTotalTokens());
        usage.put("estimatedCost", cost.toPlainString());
        result.put("usage", usage);

        return result;
    }

    private BigDecimal calculateCost(int promptTokens, int completionTokens) {
        String provider = aiProvider.getName();
        if ("bailian".equals(provider)) {
            // 百炼通义千问定价: 输入 ¥0.5/1M tokens, 输出 ¥2/1M tokens (以 qwen-plus 为例)
            BigDecimal inputCost = BigDecimal.valueOf(promptTokens).multiply(BigDecimal.valueOf(0.5)).divide(BigDecimal.valueOf(1000000), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal outputCost = BigDecimal.valueOf(completionTokens).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(1000000), 6, BigDecimal.ROUND_HALF_UP);
            return inputCost.add(outputCost);
        }
        // DeepSeek 定价: 输入 ¥1/1M tokens, 输出 ¥2/1M tokens
        BigDecimal inputCost = BigDecimal.valueOf(promptTokens).multiply(BigDecimal.valueOf(1)).divide(BigDecimal.valueOf(1000000), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens).multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(1000000), 6, BigDecimal.ROUND_HALF_UP);
        return inputCost.add(outputCost);
    }

    private void logUsage(Long userId, String feature, int promptTokens, int completionTokens,
                          int totalTokens, BigDecimal cost, Long latencyMs, String status) {
        AiUsageLog logEntry = new AiUsageLog();
        logEntry.setUserId(userId);
        logEntry.setFeature(feature);
        logEntry.setProvider(aiProvider.getName());
        logEntry.setModel(aiProvider.getModelName());
        logEntry.setPromptTokens(promptTokens);
        logEntry.setCompletionTokens(completionTokens);
        logEntry.setTotalTokens(totalTokens);
        logEntry.setCostYuan(cost);
        logEntry.setLatencyMs(latencyMs != null ? latencyMs.intValue() : 0);
        logEntry.setStatus(status);
        usageLogMapper.insert(logEntry);
    }

    public List<AiConversation> getConversations(Long userId, int page, int size) {
        IPage<AiConversation> convPage = conversationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime));
        return convPage.getRecords();
    }

    public List<AiMessage> getMessages(String conversationId, Long userId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        return messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime));
    }

    public void deleteConversation(String conversationId, Long userId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        // 删除会话下的所有消息
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));
        // 删除会话
        conversationMapper.deleteById(conversationId);
    }

    public Map<String, Object> getUsage(Long userId) {
        // 返回总计
        List<AiUsageLog> logs = usageLogMapper.selectList(
                new LambdaQueryWrapper<AiUsageLog>()
                        .eq(AiUsageLog::getUserId, userId)
                        .eq(AiUsageLog::getStatus, "success"));

        int totalCalls = logs.size();
        int totalTokens = logs.stream().mapToInt(AiUsageLog::getTotalTokens).sum();
        BigDecimal totalCost = logs.stream().map(AiUsageLog::getCostYuan).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按功能类型分组统计
        Map<String, List<AiUsageLog>> byFeature = logs.stream()
                .collect(Collectors.groupingBy(AiUsageLog::getFeature));
        Map<String, Map<String, Object>> byFeatureResult = new HashMap<>();
        for (Map.Entry<String, List<AiUsageLog>> entry : byFeature.entrySet()) {
            Map<String, Object> featureStat = new HashMap<>();
            featureStat.put("calls", entry.getValue().size());
            featureStat.put("tokens", entry.getValue().stream().mapToInt(AiUsageLog::getTotalTokens).sum());
            featureStat.put("costYuan", entry.getValue().stream()
                    .map(AiUsageLog::getCostYuan).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
            byFeatureResult.put(entry.getKey(), featureStat);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCalls", totalCalls);
        result.put("totalTokens", totalTokens);
        result.put("totalCostYuan", totalCost.toPlainString());
        result.put("byFeature", byFeatureResult);
        return result;
    }
}

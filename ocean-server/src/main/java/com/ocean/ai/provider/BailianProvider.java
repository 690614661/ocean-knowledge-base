package com.ocean.ai.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ocean.ai.model.AiRequest;
import com.ocean.ai.model.AiResponse;
import com.ocean.ai.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * 阿里云百炼（DashScope）AI 供应商实现
 * 支持通义千问（Qwen）系列模型
 * API 文档：https://help.aliyun.com/zh/model-studio/
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "bailian")
public class BailianProvider implements AiProvider {

    @Value("${BAILIAN_API_KEY:}")
    private String apiKey;

    @Value("${ai.bailian.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    @Value("${ai.bailian.model:qwen-plus}")
    private String defaultModel;

    @Value("${ai.bailian.max-tokens:4096}")
    private Integer maxTokens;

    @Value("${ai.bailian.temperature:0.7}")
    private Double temperature;

    @Value("${ai.bailian.timeout:60}")
    private Integer timeout;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int ms = timeout * 1000;
        factory.setConnectTimeout(ms);
        factory.setReadTimeout(ms);
        this.restTemplate = new RestTemplate(factory);
        log.info("BailianProvider initialized, apiKey={}", apiKey != null && !apiKey.isEmpty() ? "***set***" : "EMPTY");
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                // 构建请求体
                JSONObject body = new JSONObject();
                body.put("model", request.getModel() != null ? request.getModel() : defaultModel);

                // 构建 messages
                JSONArray messages = new JSONArray();

                if (request.getSystemPrompt() != null) {
                    JSONObject sysMsg = new JSONObject();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", request.getSystemPrompt());
                    messages.add(sysMsg);
                }

                if (request.getMessages() != null) {
                    for (ChatMessage msg : request.getMessages()) {
                        JSONObject msgObj = new JSONObject();
                        msgObj.put("role", msg.getRole());
                        msgObj.put("content", msg.getContent());
                        messages.add(msgObj);
                    }
                }

                // 百炼 API 的 input 结构
                JSONObject input = new JSONObject();
                input.put("messages", messages);
                body.put("input", input);

                // 参数
                JSONObject parameters = new JSONObject();
                parameters.put("result_format", "message");
                parameters.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);
                parameters.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
                body.put("parameters", parameters);

                // 请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

                // 调用百炼 API
                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/api/v1/services/aigc/text-generation/generation",
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                long latency = System.currentTimeMillis() - startTime;
                String responseBody = response.getBody();

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("百炼API调用失败: status={}, body={}", response.getStatusCode(), responseBody);
                    throw new RuntimeException("AI服务暂时不可用");
                }

                JSONObject result = JSON.parseObject(responseBody);
                JSONObject output = result.getJSONObject("output");
                JSONArray choices = output.getJSONArray("choices");
                String content = choices.getJSONObject(0).getJSONObject("message").getString("content");

                JSONObject usage = result.getJSONObject("usage");

                AiResponse aiResponse = new AiResponse();
                aiResponse.setContent(content);
                // 百炼返回 input_tokens / output_tokens
                aiResponse.setPromptTokens(usage.getInteger("input_tokens"));
                aiResponse.setCompletionTokens(usage.getInteger("output_tokens"));
                aiResponse.setTotalTokens(usage.getInteger("total_tokens"));
                aiResponse.setModel(result.getString("model"));
                aiResponse.setProvider(getName());
                aiResponse.setLatencyMs(latency);

                return aiResponse;
            } catch (Exception e) {
                lastException = e;
                log.warn("百炼API第{}/2次调用失败: {}", attempt, e.getMessage());
                if (attempt < 2) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }

        log.error("百炼API两次调用均失败", lastException);
        throw new RuntimeException("AI响应超时，请稍后重试");
    }

    @Override
    public String getName() {
        return "bailian";
    }

    @Override
    public String getModelName() {
        return defaultModel;
    }
}

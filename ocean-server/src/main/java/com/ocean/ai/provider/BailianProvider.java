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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

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
    public void streamChat(AiRequest request, Consumer<String> onChunk,
                           Consumer<String> onUsage, Runnable onComplete,
                           Consumer<Throwable> onError) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", request.getModel() != null ? request.getModel() : defaultModel);

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

            JSONObject input = new JSONObject();
            input.put("messages", messages);
            body.put("input", input);

            JSONObject parameters = new JSONObject();
            parameters.put("result_format", "message");
            parameters.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);
            parameters.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
            parameters.put("incremental_output", true);
            body.put("parameters", parameters);

            URL url = new URL(baseUrl + "/api/v1/services/aigc/text-generation/generation");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("X-DashScope-SSE", "enable");
            connection.setDoOutput(true);
            connection.setConnectTimeout(timeout * 1000);
            connection.setReadTimeout(0);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorBody = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBody.append(line);
                    }
                    log.error("百炼SSE调用失败: status={}, body={}", statusCode, errorBody);
                }
                onError.accept(new RuntimeException("AI服务暂时不可用"));
                return;
            }

            StringBuilder fullContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    // 跳过 SSE 注释行 (:HTTP_STATUS/200) 和 id/event 行
                    if (line.startsWith(":") || line.startsWith("event:") || line.startsWith("id:")) continue;

                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (data.isEmpty()) continue;

                        try {
                            JSONObject chunk = JSON.parseObject(data);
                            JSONObject output = chunk.getJSONObject("output");
                            if (output == null) continue;

                            // Bailian 格式: output.choices[0].message.content
                            JSONArray choices = output.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject choice = choices.getJSONObject(0);
                                JSONObject message = choice.getJSONObject("message");
                                if (message != null) {
                                    String content = message.getString("content");
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        JSONObject chunkData = new JSONObject();
                                        chunkData.put("content", content);
                                        onChunk.accept(chunkData.toJSONString());
                                    }
                                }
                                String finishReason = choice.getString("finish_reason");
                                if ("stop".equals(finishReason)) break;
                            }
                        } catch (Exception e) {
                            log.warn("解析百炼SSE失败: data={}", data, e);
                        }
                    }
                }
            }

            onComplete.run();

        } catch (Exception e) {
            log.error("百炼SSE流式调用失败", e);
            onError.accept(e);
        }
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

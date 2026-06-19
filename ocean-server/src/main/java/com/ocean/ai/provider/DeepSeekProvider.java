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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek", matchIfMissing = true)
public class DeepSeekProvider implements AiProvider {

    @Value("${DEEPSEEK_API_KEY:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.model}")
    private String defaultModel;

    @Value("${ai.deepseek.max-tokens}")
    private Integer maxTokens;

    @Value("${ai.deepseek.temperature}")
    private Double temperature;

    @Value("${ai.deepseek.timeout:60}")
    private Integer timeout;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int ms = timeout * 1000;
        factory.setConnectTimeout(ms);
        factory.setReadTimeout(ms);
        this.restTemplate = new RestTemplate(factory);
        log.info("DeepSeekProvider initialized, apiKey={}", apiKey != null && !apiKey.isEmpty() ? "***set***" : "EMPTY");
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                JSONObject body = new JSONObject();
                body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
                body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);
                body.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);

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

                body.put("messages", messages);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/chat/completions",
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                long latency = System.currentTimeMillis() - startTime;
                String responseBody = response.getBody();

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("DeepSeek API调用失败: status={}, body={}", response.getStatusCode(), responseBody);
                    throw new RuntimeException("AI服务暂时不可用");
                }

                JSONObject result = JSON.parseObject(responseBody);
                JSONArray choices = result.getJSONArray("choices");
                String content = choices.getJSONObject(0).getJSONObject("message").getString("content");

                JSONObject usage = result.getJSONObject("usage");

                AiResponse aiResponse = new AiResponse();
                aiResponse.setContent(content);
                aiResponse.setPromptTokens(usage.getInteger("prompt_tokens"));
                aiResponse.setCompletionTokens(usage.getInteger("completion_tokens"));
                aiResponse.setTotalTokens(usage.getInteger("total_tokens"));
                aiResponse.setModel(result.getString("model"));
                aiResponse.setProvider(getName());
                aiResponse.setLatencyMs(latency);

                return aiResponse;
            } catch (Exception e) {
                lastException = e;
                log.warn("DeepSeek API第{}/2次调用失败: {}", attempt, e.getMessage());
                if (attempt < 2) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }

        log.error("DeepSeek API两次调用均失败", lastException);
        throw new RuntimeException("AI响应超时，请稍后重试");
    }

    @Override
    public void streamChat(AiRequest request, Consumer<String> onChunk,
                           Consumer<String> onUsage, Runnable onComplete,
                           Consumer<Throwable> onError) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
            body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxTokens);
            body.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
            body.put("stream", true);

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
            body.put("messages", messages);

            URL url = new URL(baseUrl + "/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
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
                    log.error("DeepSeek SSE调用失败: status={}, body={}", statusCode, errorBody);
                }
                onError.accept(new RuntimeException("AI服务暂时不可用"));
                return;
            }

            StringBuilder fullContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            JSONObject chunk = JSON.parseObject(data);
                            JSONArray choices = chunk.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                if (delta != null) {
                                    String content = delta.getString("content");
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        // Send content chunk as JSON
                                        JSONObject chunkData = new JSONObject();
                                        chunkData.put("content", content);
                                        onChunk.accept(chunkData.toJSONString());
                                    }
                                }
                                // 检查最后一个 chunk 是否有 usage
                                JSONObject usage = choices.getJSONObject(0).getJSONObject("usage_info");
                                if (usage == null) {
                                    usage = chunk.getJSONObject("usage");
                                }
                                if (usage != null && onUsage != null) {
                                    onUsage.accept(usage.toJSONString());
                                }
                            }
                        } catch (Exception e) {
                            log.warn("解析SSE chunk失败: {}", data, e);
                        }
                    }
                }
            }

            onComplete.run();

        } catch (Exception e) {
            log.error("DeepSeek SSE流式调用失败", e);
            onError.accept(e);
        }
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public String getModelName() {
        return defaultModel;
    }
}
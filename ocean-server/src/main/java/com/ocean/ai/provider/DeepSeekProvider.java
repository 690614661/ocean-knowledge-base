package com.ocean.ai.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ocean.ai.model.AiRequest;
import com.ocean.ai.model.AiResponse;
import com.ocean.ai.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DeepSeekProvider implements AiProvider {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.model}")
    private String defaultModel;

    @Value("${ai.deepseek.max-tokens}")
    private Integer maxTokens;

    @Value("${ai.deepseek.temperature}")
    private Double temperature;

    @Value("${ai.deepseek.timeout}")
    private Integer timeout;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();

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

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                long latency = System.currentTimeMillis() - startTime;
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("DeepSeek API调用失败: status={}, body={}", response.code(), responseBody);
                    AiResponse aiResponse = new AiResponse();
                    aiResponse.setProvider(getName());
                    aiResponse.setLatencyMs(latency);
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
            }
        } catch (IOException e) {
            log.error("DeepSeek API调用异常", e);
            throw new RuntimeException("AI响应超时，请稍后重试");
        }
    }

    @Override
    public String getName() {
        return "deepseek";
    }
}

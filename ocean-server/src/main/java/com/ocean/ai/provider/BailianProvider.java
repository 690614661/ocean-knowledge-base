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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.function.Consumer;

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

    @Value("${ocean.file.local.upload-path:./upload/}")
    private String localUploadPath;

    private static final String VL_MODEL = "qwen-vl-plus";

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

        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isEmpty();
        String model = hasImage ? VL_MODEL : (request.getModel() != null ? request.getModel() : defaultModel);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                JSONObject body = new JSONObject();
                body.put("model", model);

                JSONArray messages = new JSONArray();
                if (request.getSystemPrompt() != null && !hasImage) {
                    JSONObject sysMsg = new JSONObject();
                    sysMsg.put("role", "system");
                    sysMsg.put("content", request.getSystemPrompt());
                    messages.add(sysMsg);
                }

                if (request.getMessages() != null) {
                    for (ChatMessage msg : request.getMessages()) {
                        JSONObject msgObj = new JSONObject();
                        msgObj.put("role", msg.getRole());

                        if (hasImage && "user".equals(msg.getRole())) {
                            JSONArray contentArray = new JSONArray();
                            JSONObject textPart = new JSONObject();
                            textPart.put("text", msg.getContent());
                            contentArray.add(textPart);
                            JSONObject imagePart = new JSONObject();
                            imagePart.put("image", toBase64Image(request.getImageUrl()));
                            contentArray.add(imagePart);
                            msgObj.put("content", contentArray);
                        } else {
                            msgObj.put("content", msg.getContent());
                        }
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
                body.put("parameters", parameters);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + (hasImage ? "/api/v1/services/aigc/multimodal-generation/generation" : "/api/v1/services/aigc/text-generation/generation"),
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
                Object contentObj = choices.getJSONObject(0).getJSONObject("message").get("content");
                // 多模态API返回的content可能是JSON数组，如 [{"text":"..."}]
                String content;
                if (contentObj instanceof JSONArray) {
                    JSONArray parts = (JSONArray) contentObj;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.size(); i++) {
                        JSONObject part = parts.getJSONObject(i);
                        if (part.containsKey("text")) {
                            sb.append(part.getString("text"));
                        }
                    }
                    content = sb.toString();
                } else {
                    content = (String) contentObj;
                }

                JSONObject usage = result.getJSONObject("usage");

                AiResponse aiResponse = new AiResponse();
                aiResponse.setContent(content);
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
            boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isEmpty();
            String model = hasImage ? VL_MODEL : (request.getModel() != null ? request.getModel() : defaultModel);

            JSONObject body = new JSONObject();
            body.put("model", model);

            JSONArray messages = new JSONArray();
            if (request.getSystemPrompt() != null && !hasImage) {
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", request.getSystemPrompt());
                messages.add(sysMsg);
            }
            if (request.getMessages() != null) {
                for (ChatMessage msg : request.getMessages()) {
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", msg.getRole());
                    if (hasImage && "user".equals(msg.getRole())) {
                        JSONArray contentArray = new JSONArray();
                        JSONObject textPart = new JSONObject();
                        textPart.put("text", msg.getContent());
                        contentArray.add(textPart);
                        JSONObject imagePart = new JSONObject();
                        imagePart.put("image", toBase64Image(request.getImageUrl()));
                        contentArray.add(imagePart);
                        msgObj.put("content", contentArray);
                    } else {
                        msgObj.put("content", msg.getContent());
                    }
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

            URL url = new URL(baseUrl + (hasImage ? "/api/v1/services/aigc/multimodal-generation/generation" : "/api/v1/services/aigc/text-generation/generation"));
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
                    while ((line = errorReader.readLine()) != null) { errorBody.append(line); }
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
                    if (line.startsWith(":") || line.startsWith("event:") || line.startsWith("id:")) continue;
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (data.isEmpty()) continue;
                        try {
                            JSONObject chunk = JSON.parseObject(data);
                            JSONObject output = chunk.getJSONObject("output");
                            if (output == null) continue;
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
                            log.warn("解析百炼SSE失败", e);
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
    public String getName() { return "bailian"; }

    @Override
    public String getModelName() { return defaultModel; }

    private String toBase64Image(String imageUrl) {
        if (imageUrl == null || imageUrl.startsWith("data:")) return imageUrl;
        try {
            byte[] imageBytes;
            if (imageUrl.startsWith("/")) {
                // 本地相对路径（如 /files/editor/uuid.jpg），从文件系统读取
                String relativePath = imageUrl.startsWith("/files/") ? imageUrl.substring("/files/".length()) : imageUrl.substring(1);
                Path filePath = Paths.get(localUploadPath, relativePath).normalize();
                imageBytes = Files.readAllBytes(filePath);
            } else {
                // 远程 URL，通过 HTTP 下载
                java.net.URL url = new java.net.URL(imageUrl);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream is = url.openStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
                }
                imageBytes = baos.toByteArray();
            }
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String ext = imageUrl.contains(".") ? imageUrl.substring(imageUrl.lastIndexOf(".") + 1).toLowerCase() : "jpg";
            String mime;
            switch (ext) {
                case "png": mime = "image/png"; break;
                case "gif": mime = "image/gif"; break;
                case "webp": mime = "image/webp"; break;
                default: mime = "image/jpeg";
            }
            return "data:" + mime + ";base64," + base64;
        } catch (Exception e) {
            log.warn("图片下载失败，使用原始URL", e);
            return imageUrl;
        }
    }
}

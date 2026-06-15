package com.ocean.ai.model;

import lombok.Data;

import java.util.List;

@Data
public class AiRequest {
    private String systemPrompt;
    private List<ChatMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}

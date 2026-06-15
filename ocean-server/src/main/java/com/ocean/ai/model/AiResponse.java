package com.ocean.ai.model;

import lombok.Data;

@Data
public class AiResponse {
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String model;
    private String provider;
    private Long latencyMs;
}

package com.ocean.ai.provider;

import com.ocean.ai.model.AiRequest;
import com.ocean.ai.model.AiResponse;

public interface AiProvider {
    AiResponse chat(AiRequest request);
    String getName();
    String getModelName();
}

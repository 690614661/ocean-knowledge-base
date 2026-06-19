package com.ocean.ai.provider;

import com.ocean.ai.model.AiRequest;
import com.ocean.ai.model.AiResponse;

import java.util.function.Consumer;

public interface AiProvider {
    AiResponse chat(AiRequest request);
    String getName();
    String getModelName();

    /**
     * 流式对话（SSE）
     * @param request 请求参数
     * @param onChunk 每段内容回调
     * @param onUsage 用量信息回调（JSON字符串）
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    default void streamChat(AiRequest request, Consumer<String> onChunk,
                            Consumer<String> onUsage, Runnable onComplete,
                            Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException(getName() + " 不支持流式输出"));
    }
}

package com.suilearn.api.agent.llm;

import java.util.stream.Stream;

public interface LlmClient {
    Stream<LlmChunk> stream(LlmRequest request);

    default LlmResponse chat(LlmRequest request) {
        return LlmResponses.aggregate(stream(request));
    }
}

package com.suilearn.api.ai.application;

public interface ChatPort {
    ChatResponse chat(ChatRequest request);

    record ChatRequest(String systemPrompt, String userPrompt) {
    }

    record ChatResponse(String text, String model) {
    }
}

package com.suilearn.api.ai.infrastructure.springai;

import com.suilearn.api.ai.application.ChatPort;

public class SpringAiChatAdapter implements ChatPort {
    @Override
    public ChatResponse chat(ChatRequest request) {
        throw new UnsupportedOperationException("Spring AI chat adapter boundary is defined but not enabled yet");
    }
}

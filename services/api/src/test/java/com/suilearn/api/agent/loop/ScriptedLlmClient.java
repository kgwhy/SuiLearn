package com.suilearn.api.agent.loop;

import com.suilearn.api.agent.llm.LlmChunk;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmRequest;
import com.suilearn.api.agent.llm.LlmResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class ScriptedLlmClient implements LlmClient {
    private final List<LlmResponse> script;
    private final AtomicInteger calls = new AtomicInteger();
    private final LlmResponse exhausted;

    public ScriptedLlmClient(List<LlmResponse> script, LlmResponse exhausted) {
        this.script = new ArrayList<>(script);
        this.exhausted = exhausted;
    }

    int calls() {
        return calls.get();
    }

    @Override
    public Stream<LlmChunk> stream(LlmRequest request) {
        int index = calls.getAndIncrement();
        LlmResponse response = index < script.size() ? script.get(index) : exhausted;
        return Stream.of(new LlmChunk(response.content(), List.of(), response.usage(), response.finishReason()))
            .map(chunk -> chunk);
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        int index = calls.getAndIncrement();
        return index < script.size() ? script.get(index) : exhausted;
    }
}

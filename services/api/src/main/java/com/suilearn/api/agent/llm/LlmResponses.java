package com.suilearn.api.agent.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class LlmResponses {
    private LlmResponses() {}

    public static LlmResponse aggregate(Stream<LlmChunk> chunks) {
        var content = new StringBuilder();
        var toolCalls = new LinkedHashMap<Integer, LlmToolCallDelta>();
        var usage = LlmUsage.none();
        var finishReason = "";
        for (LlmChunk chunk : (Iterable<LlmChunk>) chunks::iterator) {
            content.append(chunk.contentDelta());
            for (LlmToolCallDelta delta : chunk.toolCallDeltas()) {
                toolCalls.merge(delta.index(), delta, LlmResponses::merge);
            }
            if (chunk.usage() != null) {
                usage = chunk.usage();
            }
            if (chunk.finishReason() != null && !chunk.finishReason().isBlank()) {
                finishReason = chunk.finishReason();
            }
        }
        var calls = toolCalls.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new LlmToolCall(entry.getValue().id(), entry.getValue().name(),
                entry.getValue().argumentsDelta()))
            .toList();
        return new LlmResponse(content.toString(), calls, usage, finishReason);
    }

    private static LlmToolCallDelta merge(LlmToolCallDelta left, LlmToolCallDelta right) {
        return new LlmToolCallDelta(left.index(),
            right.id() == null || right.id().isBlank() ? left.id() : right.id(),
            right.name() == null || right.name().isBlank() ? left.name() : right.name(),
            left.argumentsDelta() + right.argumentsDelta());
    }
}

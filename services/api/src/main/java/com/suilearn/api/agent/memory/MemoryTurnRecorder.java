package com.suilearn.api.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records the bounded, sanitized memory side effects for a finished AgentLoop turn.
 * It runs after the loop has already published its terminal event, so any failure
 * here is logged and must be swallowed by the orchestrator; it cannot change the
 * turn outcome.
 */
public final class MemoryTurnRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryTurnRecorder.class);
    private static final int RESULT_EXCERPT_MAX_LENGTH = 1200;

    private final MemoryTraceRecorder traces;
    private final MemorySnapshotRecorder snapshots;
    private final MemoryConsolidator consolidator;
    private final ObjectMapper objectMapper;

    public MemoryTurnRecorder(MemoryTraceRecorder traces, MemorySnapshotRecorder snapshots,
                              MemoryConsolidator consolidator, ObjectMapper objectMapper) {
        this.traces = traces;
        this.snapshots = snapshots;
        this.consolidator = consolidator;
        this.objectMapper = objectMapper;
    }

    public void recordTerminalTurn(TurnContext context, String outcome, int toolCalls,
                                   LlmUsage usage, String resultContent) {
        String kind = outcome == null || outcome.isBlank() ? "turn_finished" : "turn_" + outcome.toLowerCase(java.util.Locale.ROOT);
        var tracePayload = new LinkedHashMap<String, Object>();
        tracePayload.put("turnId", context.turnId());
        tracePayload.put("capability", context.capability());
        tracePayload.put("status", outcome);
        tracePayload.put("toolCalls", toolCalls);
        tracePayload.put("promptTokens", usage == null ? 0L : usage.promptTokens());
        tracePayload.put("completionTokens", usage == null ? 0L : usage.completionTokens());
        traces.append(context.learnerId(), context.turnId(), "turn", kind, writeJson(tracePayload));

        if (!"COMPLETED".equals(outcome)) {
            return;
        }
        var snapshotPayload = new LinkedHashMap<String, Object>();
        snapshotPayload.put("turnId", context.turnId());
        snapshotPayload.put("capability", context.capability());
        snapshotPayload.put("status", outcome);
        snapshotPayload.put("toolCalls", toolCalls);
        snapshotPayload.put("resultExcerpt", excerpt(resultContent));
        String content = writeJson(snapshotPayload);
        String fingerprint = MemoryFingerprint.of(content);
        boolean recorded = snapshots.record(context.learnerId(), "turn", "turn:" + context.turnId(),
            content, fingerprint);
        if (recorded) {
            consolidator.submitUpdate(context.learnerId(), "turn", context.turnId());
        } else {
            LOG.debug("Turn snapshot already recorded for turnId={}", context.turnId());
        }
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize memory payload", exception);
        }
    }

    private static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String stripped = content.strip();
        return stripped.length() <= RESULT_EXCERPT_MAX_LENGTH
            ? stripped
            : stripped.substring(0, RESULT_EXCERPT_MAX_LENGTH);
    }
}

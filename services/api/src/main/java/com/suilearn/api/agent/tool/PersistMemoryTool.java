package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.memory.MemoryCandidate;
import com.suilearn.api.agent.memory.MemoryFingerprint;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryType;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PersistMemoryTool implements Tool {
    private final MemoryManager memory;

    public PersistMemoryTool(MemoryManager memory) {
        this.memory = memory;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.PERSIST_MEMORY,
            "Persist one bounded learner-memory candidate through the existing promotion policy.",
            Map.of("type", "object", "properties", Map.of(
                "memoryType", Map.of("type", "string", "enum", List.of("GOAL", "PREFERENCE", "WEAKNESS", "MASTERY")),
                "content", Map.of("type", "string", "minLength", 1, "maxLength", 500),
                "confidence", Map.of("type", "number", "minimum", 0.5, "maximum", 1.0),
                "sourceRef", Map.of("type", "string", "minLength", 1),
                "sourceRunId", Map.of("type", "string")
            ), "required", List.of("memoryType", "content", "confidence", "sourceRef")),
            false, Set.of("memory"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        MemoryType type = MemoryType.valueOf(ToolArguments.requiredString(args, "memoryType", 32));
        String content = ToolArguments.requiredString(args, "content", 500);
        double confidence = ToolArguments.decimal(args, "confidence", 0.5d, 0.5d, 1.0d);
        String sourceRef = ToolArguments.requiredString(args, "sourceRef", 256);
        String sourceRunId = ToolArguments.optionalString(args, "sourceRunId", 128);
        if (memory == null) {
            return new ToolResult("Memory persistence is unavailable.", List.of(),
                Map.of("code", "MEMORY_UNAVAILABLE"), false, null);
        }
        try {
            var candidate = new MemoryCandidate(context.learnerId(), type, content,
                MemoryFingerprint.of(content), confidence,
                sourceRunId == null ? context.turnId() : sourceRunId, sourceRef);
            var result = memory.promote(context.learnerId(), candidate);
            var metadata = new java.util.LinkedHashMap<String, Object>();
            metadata.put("status", result.status().name());
            if (result.memoryId() != null) {
                metadata.put("memoryId", result.memoryId());
            }
            if (result.rejection() != null) {
                metadata.put("rejection", result.rejection().name());
            }
            return new ToolResult("Memory persistence completed with status " + result.status() + ".",
                List.of(), metadata, true, null);
        } catch (RuntimeException exception) {
            return new ToolResult("Memory persistence failed.", List.of(),
                Map.of("code", "MEMORY_PERSIST_FAILED"), false, null);
        }
    }
}

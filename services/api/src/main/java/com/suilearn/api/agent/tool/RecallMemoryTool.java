package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.memory.MemoryL2DocRepository;
import com.suilearn.api.agent.memory.MemoryL3DocRepository;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryType;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RecallMemoryTool implements Tool {
    private final MemoryManager memory;
    private final MemoryL2DocRepository l2;
    private final MemoryL3DocRepository l3;

    public RecallMemoryTool(MemoryManager memory) {
        this(memory, null, null);
    }

    public RecallMemoryTool(MemoryManager memory, MemoryL2DocRepository l2, MemoryL3DocRepository l3) {
        this.memory = memory;
        this.l2 = l2;
        this.l3 = l3;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.RECALL_MEMORY,
            "Recall session and long-term learner memory relevant to a query.",
            Map.of("type", "object", "properties", Map.of(
                "query", Map.of("type", "string", "minLength", 1),
                "memoryTypes", Map.of("type", "array", "items", Map.of(
                    "type", "string", "enum", List.of("GOAL", "PREFERENCE", "WEAKNESS", "MASTERY")))
            ), "required", List.of("query")),
            false, Set.of("memory"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        String query = ToolArguments.requiredString(args, "query", 2000);
        Set<MemoryType> types = memoryTypes(args);
        if (memory == null) {
            return new ToolResult("Memory recall is unavailable.", List.of(),
                Map.of("code", "MEMORY_UNAVAILABLE"), false, null);
        }
        try {
            var result = memory.recall(context.learnerId(), types, query);
            var memories = result.memories().stream().map(scored -> {
                var item = new LinkedHashMap<String, Object>();
                var source = scored.memory();
                item.put("id", source.id());
                item.put("type", source.memoryType().name());
                item.put("content", source.content());
                item.put("confidence", source.confidence());
                item.put("sourceRef", source.sourceRef());
                item.put("score", scored.score());
                return item;
            }).toList();
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("status", result.status().name());
            if (result.detail() != null) {
                metadata.put("detail", result.detail());
            }
            metadata.put("memories", memories);
            if (l2 != null) {
                metadata.put("l2Docs", l2.findByLearnerIdOrderByUpdatedAtDesc(context.learnerId()).stream()
                    .limit(3).map(doc -> doc.getSurface() + ": " + doc.getContentMd()).toList());
            }
            if (l3 != null) {
                metadata.put("l3Slots", l3.findByLearnerIdOrderBySlotAsc(context.learnerId()).stream()
                    .limit(4).map(doc -> doc.getSlot() + ": " + doc.getContentMd()).toList());
            }
            return new ToolResult("Recalled " + memories.size() + " memory item(s).", List.of(), metadata, true, null);
        } catch (RuntimeException exception) {
            return new ToolResult("Memory recall failed.", List.of(),
                Map.of("code", "MEMORY_RECALL_FAILED"), false, null);
        }
    }

    private static Set<MemoryType> memoryTypes(Map<String, Object> args) {
        Object raw = args.get("memoryTypes");
        if (raw == null) {
            return MemoryType.allowed();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("memoryTypes must be a list");
        }
        Set<MemoryType> types = list.stream().map(value -> MemoryType.valueOf(String.valueOf(value)))
            .collect(Collectors.toUnmodifiableSet());
        if (types.isEmpty() || !MemoryType.allowed().containsAll(types)) {
            throw new IllegalArgumentException("memoryTypes contains invalid values");
        }
        return types;
    }
}

package com.suilearn.api.agent.loop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.TurnContext;
import com.suilearn.api.agent.runtime.TurnEventSink;
import com.suilearn.api.agent.runtime.ToolRegistry;
import com.suilearn.api.agent.tool.ForbiddenAgentActionException;
import com.suilearn.api.agent.tool.ToolDefinition;
import com.suilearn.api.agent.tool.ToolResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ToolDispatcher {
    public static final int MAX_PARALLEL_CALLS = 8;

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public ToolDispatcher(ToolRegistry registry, ObjectMapper objectMapper) {
        this(registry, objectMapper, Executors.newVirtualThreadPerTaskExecutor());
    }

    ToolDispatcher(ToolRegistry registry, ObjectMapper objectMapper, ExecutorService executor) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    public List<ToolExecution> dispatch(TurnContext context, CapabilityManifest manifest,
                                        List<LlmToolCall> calls, TurnEventSink events) {
        List<LlmToolCall> unique = deduplicate(calls);
        if (unique.size() > MAX_PARALLEL_CALLS) {
            unique = unique.subList(0, MAX_PARALLEL_CALLS);
        }
        var executions = new ArrayList<ToolExecution>();
        var futures = new ArrayList<CompletableFuture<ToolExecution>>();
        for (LlmToolCall call : unique) {
            futures.add(CompletableFuture.supplyAsync(() -> executeOne(context, manifest, call, events), executor));
        }
        for (var future : futures) {
            ToolExecution execution = future.join();
            executions.add(execution);
            if (execution.result().pauseForUser() != null) {
                throw new AskUserPauseException(execution.result().pauseForUser());
            }
        }
        return executions;
    }

    private ToolExecution executeOne(TurnContext context, CapabilityManifest manifest,
                                     LlmToolCall call, TurnEventSink events) {
        events.publish(EventType.TOOL_CALL, context.capability(), "tool",
            "Calling tool " + call.name(), Map.of("tool", call.name(), "callId", call.id()));
        var result = validateAndExecute(context, manifest, call);
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("tool", call.name());
        metadata.put("success", result.success());
        if (!result.metadata().isEmpty() && result.metadata().get("code") != null) {
            metadata.put("code", result.metadata().get("code"));
        }
        events.publish(EventType.TOOL_RESULT, context.capability(), "tool",
            result.content(), metadata);
        return new ToolExecution(call, result);
    }

    private ToolResult validateAndExecute(TurnContext context, CapabilityManifest manifest, LlmToolCall call) {
        final com.suilearn.api.agent.tool.Tool tool;
        try {
            tool = registry.require(manifest, call.name());
        } catch (ForbiddenAgentActionException forbidden) {
            return new ToolResult("Tool is not allowed for this capability.", List.of(),
                Map.of("code", "FORBIDDEN_AGENT_ACTION"), false, null);
        } catch (IllegalArgumentException unknown) {
            return new ToolResult("Unknown tool.", List.of(), Map.of("code", "UNKNOWN_TOOL"), false, null);
        }
        ToolDefinition definition = tool.definition();
        Map<String, Object> args;
        try {
            args = call.arguments() == null || call.arguments().isBlank()
                ? new LinkedHashMap<>()
                : objectMapper.readValue(call.arguments(), Map.class);
        } catch (JsonProcessingException invalid) {
            return new ToolResult("Tool arguments are not valid JSON.", List.of(),
                Map.of("code", "INVALID_TOOL_ARGUMENTS"), false, null);
        }
        List<String> missing = missingRequired(definition, args);
        if (!missing.isEmpty()) {
            return new ToolResult("Missing required tool arguments: " + String.join(", ", missing),
                List.of(), Map.of("code", "MISSING_REQUIRED_ARGUMENT", "missing", missing), false, null);
        }
        return tool.execute(context, args);
    }

    private static List<LlmToolCall> deduplicate(List<LlmToolCall> calls) {
        var unique = new LinkedHashMap<String, LlmToolCall>();
        for (LlmToolCall call : calls) {
            unique.putIfAbsent(call.name() + '\u0000' + call.arguments(), call);
        }
        return List.copyOf(unique.values());
    }

    @SuppressWarnings("unchecked")
    private static List<String> missingRequired(ToolDefinition definition, Map<String, Object> args) {
        Object parameters = definition.parameters();
        if (!(parameters instanceof Map<?, ?> schema)) {
            return List.of();
        }
        Object rawRequired = schema.get("required");
        if (!(rawRequired instanceof List<?> required)) {
            return List.of();
        }
        var missing = new ArrayList<String>();
        for (Object item : required) {
            String name = String.valueOf(item);
            if (!args.containsKey(name) || args.get(name) == null || String.valueOf(args.get(name)).isBlank()) {
                missing.add(name);
            }
        }
        return missing;
    }
}

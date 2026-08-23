package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.runtime.TurnContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AskUserTool implements Tool {
    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.ASK_USER,
            "Ask the learner for missing goals, difficulty, or answer choices.",
            Map.of("type", "object", "properties", Map.of(
                "questionId", Map.of("type", "string", "minLength", 1),
                "prompt", Map.of("type", "string", "minLength", 1),
                "multiSelect", Map.of("type", "boolean", "default", false),
                "options", Map.of("type", "array", "items", Map.of(
                    "type", "object", "required", List.of("id", "label"),
                    "properties", Map.of("id", Map.of("type", "string"), "label", Map.of("type", "string"))))
            ), "required", List.of("questionId", "prompt")),
            false, Set.of("turn"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        String questionId = ToolArguments.requiredString(args, "questionId", 128);
        String prompt = ToolArguments.requiredString(args, "prompt", 4000);
        boolean multiSelect = ToolArguments.bool(args, "multiSelect", false);
        var options = ToolArguments.mapList(args, "options").stream().map(item ->
            new AskUserPayload.AskOption(ToolArguments.requiredString(item, "id", 128),
                ToolArguments.requiredString(item, "label", 512))).toList();
        return new ToolResult("", List.of(), Map.of(), false,
            new AskUserPayload(questionId, prompt, options, multiSelect));
    }
}

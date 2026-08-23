package com.suilearn.api.agent.runtime;

import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.tool.ForbiddenAgentActionException;
import com.suilearn.api.agent.tool.Tool;
import com.suilearn.api.agent.tool.ToolDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ToolRegistry {
    private final Map<String, Tool> tools;

    public ToolRegistry(Map<String, Tool> tools) {
        Objects.requireNonNull(tools, "tools");
        var copy = new LinkedHashMap<String, Tool>();
        for (var tool : tools.values()) {
            ToolDefinition definition = tool.definition();
            requireDefinition(definition);
            if (copy.putIfAbsent(definition.name(), tool) != null) {
                throw new IllegalArgumentException("duplicate tool name: " + definition.name());
            }
        }
        this.tools = Map.copyOf(copy);
    }

    public List<ToolDefinition> definitions() {
        return tools.values().stream().map(Tool::definition)
            .sorted(Comparator.comparing(ToolDefinition::name)).toList();
    }

    public Set<String> allowedTools(CapabilityManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return tools.keySet().stream()
            .filter(manifest.ownedTools()::contains)
            .collect(Collectors.toUnmodifiableSet());
    }

    public Tool require(CapabilityManifest manifest, String toolName) {
        Objects.requireNonNull(manifest, "manifest");
        if (toolName == null || toolName.isBlank()) {
            throw new ForbiddenAgentActionException();
        }
        if (!manifest.ownedTools().contains(toolName)) {
            throw new ForbiddenAgentActionException();
        }
        Tool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("unknown tool: " + toolName);
        }
        return tool;
    }

    /** OpenAI function-calling compatible tool schemas, sorted by tool name. */
    public List<Map<String, Object>> openAiSchemas() {
        return schemas(definitions());
    }

    /** Schemas limited to the tools owned by one capability. */
    public List<Map<String, Object>> openAiSchemas(CapabilityManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        return schemas(definitions().stream()
            .filter(definition -> manifest.ownedTools().contains(definition.name()))
            .toList());
    }

    private List<Map<String, Object>> schemas(List<ToolDefinition> definitions) {
        var schemas = new ArrayList<Map<String, Object>>();
        for (var definition : definitions) {
            var function = new LinkedHashMap<String, Object>();
            function.put("name", definition.name());
            function.put("description", definition.description());
            function.put("parameters", definition.parameters());
            schemas.add(Map.of("type", "function", "function", Map.copyOf(function)));
        }
        return List.copyOf(schemas);
    }

    private static void requireDefinition(ToolDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definition.name() == null || definition.name().isBlank()) {
            throw new IllegalArgumentException("tool name is required");
        }
        if (definition.parameters() == null || definition.parameters().isEmpty()) {
            throw new IllegalArgumentException("tool parameters schema is required: " + definition.name());
        }
    }
}

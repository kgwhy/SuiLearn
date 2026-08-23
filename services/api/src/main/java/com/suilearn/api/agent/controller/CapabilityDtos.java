package com.suilearn.api.agent.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CapabilityDtos {
    private CapabilityDtos() {}

    public record CapabilitiesResponse(List<CapabilityDescriptor> capabilities,
                                       List<ToolDescriptor> tools) {
        public CapabilitiesResponse {
            capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
            tools = List.copyOf(tools == null ? List.of() : tools);
        }
    }

    public record CapabilityDescriptor(String name, String description, Set<String> ownedTools) {
        public CapabilityDescriptor {
            ownedTools = Set.copyOf(ownedTools == null ? Set.of() : ownedTools);
        }
    }

    public record ToolDescriptor(String name, String description, Map<String, Object> parameters,
                                 boolean deferred, Set<String> requiredScopes) {
        public ToolDescriptor {
            parameters = Map.copyOf(parameters == null ? Map.of() : parameters);
            requiredScopes = Set.copyOf(requiredScopes == null ? Set.of() : requiredScopes);
        }
    }
}

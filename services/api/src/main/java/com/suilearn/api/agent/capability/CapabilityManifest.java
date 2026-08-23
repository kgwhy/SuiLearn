package com.suilearn.api.agent.capability;

import java.util.Objects;
import java.util.Set;

public record CapabilityManifest(String name, String description, Set<String> ownedTools) {
    public CapabilityManifest {
        name = requireText(name, "name");
        description = description == null ? "" : description.strip();
        ownedTools = Set.copyOf(ownedTools == null ? Set.of() : ownedTools);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}

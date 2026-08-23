package com.suilearn.api.agent.runtime;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.capability.Capability;
import com.suilearn.api.agent.capability.CapabilityManifest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CapabilityRegistry {
    private final Map<String, Capability> capabilities;

    public CapabilityRegistry(Map<String, Capability> capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        var copy = new LinkedHashMap<String, Capability>();
        for (var capability : capabilities.values()) {
            CapabilityManifest manifest = capability.manifest();
            if (copy.putIfAbsent(manifest.name(), capability) != null) {
                throw new IllegalArgumentException("duplicate capability name: " + manifest.name());
            }
        }
        this.capabilities = Map.copyOf(copy);
    }

    public static CapabilityRegistry builtin() {
        var copy = new LinkedHashMap<String, Capability>();
        BuiltinCapabilities.all().forEach(capability -> copy.put(capability.manifest().name(), capability));
        return new CapabilityRegistry(copy);
    }

    public Capability resolve(String name) {
        String effective = name == null || name.isBlank() ? BuiltinCapabilities.STUDY_AGENT : name.strip();
        Capability capability = capabilities.get(effective);
        if (capability == null) {
            throw new TurnApiException(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN);
        }
        return capability;
    }

    public Capability resolve(TurnContext context) {
        return resolve(context.capability());
    }

    public List<CapabilityManifest> manifests() {
        return capabilities.values().stream()
            .map(Capability::manifest)
            .sorted(Comparator.comparing(CapabilityManifest::name))
            .toList();
    }
}

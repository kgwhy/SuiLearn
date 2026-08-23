package com.suilearn.api.agent.capability;

/**
 * Capability boundary declared by the refactor plan. Change-1 only stabilizes the
 * contract; Spring registration and routing are delivered by change-2.
 */
@FunctionalInterface
public interface Capability {
    CapabilityManifest manifest();
}

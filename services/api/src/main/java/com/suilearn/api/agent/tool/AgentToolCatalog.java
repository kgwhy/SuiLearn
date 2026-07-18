package com.suilearn.api.agent.tool;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class AgentToolCatalog {
    private final Map<AgentRole, Set<AgentAction>> allowlist;

    private AgentToolCatalog(Map<AgentRole, Set<AgentAction>> allowlist) {
        var copy = new EnumMap<AgentRole, Set<AgentAction>>(AgentRole.class);
        allowlist.forEach((role, actions) -> copy.put(role, Set.copyOf(actions)));
        this.allowlist = Map.copyOf(copy);
    }

    public static AgentToolCatalog fixedMvp() {
        return new AgentToolCatalog(Map.of(
            AgentRole.SUPERVISOR, Set.of(AgentAction.KNOWLEDGE_RESEARCH, AgentAction.PRACTICE_COACH),
            AgentRole.KNOWLEDGE_RESEARCH, Set.of(AgentAction.SEARCH_KNOWLEDGE, AgentAction.READ_EVIDENCE),
            AgentRole.PRACTICE_COACH, Set.of()));
    }

    public Set<AgentRole> agentRoles() {
        return allowlist.keySet();
    }

    public Set<AgentAction> allowedActions(AgentRole role) {
        return allowlist.getOrDefault(role, Set.of());
    }

    public void requireAllowed(AgentRole role, AgentAction action) {
        if (!allowedActions(role).contains(action)) {
            throw new ForbiddenAgentActionException();
        }
    }
}

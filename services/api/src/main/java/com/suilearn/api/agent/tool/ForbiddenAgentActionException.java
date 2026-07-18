package com.suilearn.api.agent.tool;

public final class ForbiddenAgentActionException extends IllegalArgumentException {
    public ForbiddenAgentActionException() {
        super("FORBIDDEN_AGENT_ACTION");
    }
}

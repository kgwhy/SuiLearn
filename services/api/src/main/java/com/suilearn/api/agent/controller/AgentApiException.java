package com.suilearn.api.agent.controller;

public final class AgentApiException extends RuntimeException {
    private final AgentErrorCode code;

    public AgentApiException(AgentErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public AgentErrorCode code() { return code; }
}

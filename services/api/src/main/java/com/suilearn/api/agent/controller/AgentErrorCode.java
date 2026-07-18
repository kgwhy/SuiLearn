package com.suilearn.api.agent.controller;

import org.springframework.http.HttpStatus;

public enum AgentErrorCode {
    INVALID_AGENT_REQUEST(HttpStatus.BAD_REQUEST, "The Agent request is invalid."),
    AGENT_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "A knowledge scope is required."),
    AGENT_SCOPE_MISMATCH(HttpStatus.BAD_REQUEST, "The requested scopes are inconsistent."),
    AGENT_SCOPE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested knowledge scope is unavailable."),
    AGENT_FEATURE_DISABLED(HttpStatus.NOT_FOUND, "The study Agent feature is disabled."),
    AGENT_MODEL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The Agent model is unavailable."),
    AGENT_SESSION_MEMORY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Agent session memory is unavailable."),
    AGENT_DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A required Agent dependency is unavailable."),
    INVALID_MODEL_OUTPUT(HttpStatus.BAD_GATEWAY, "The model returned invalid structured output.");

    private final HttpStatus status;
    private final String safeMessage;

    AgentErrorCode(HttpStatus status, String safeMessage) {
        this.status = status;
        this.safeMessage = safeMessage;
    }

    public HttpStatus status() { return status; }
    public String safeMessage() { return safeMessage; }
}

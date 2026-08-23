package com.suilearn.api.agent.runtime;

import org.springframework.http.HttpStatus;

public enum TurnErrorCode {
    INVALID_AGENT_REQUEST(HttpStatus.BAD_REQUEST, "The Agent turn request is invalid."),
    AGENT_FEATURE_DISABLED(HttpStatus.NOT_FOUND, "The study Agent feature is disabled."),
    AGENT_WEBSOCKET_DISABLED(HttpStatus.NOT_FOUND, "The Agent WebSocket endpoint is disabled."),
    AGENT_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "A knowledge scope is required."),
    AGENT_CAPABILITY_UNKNOWN(HttpStatus.BAD_REQUEST, "The requested Agent capability is not available."),
    AGENT_TURN_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested Agent turn does not exist."),
    AGENT_TURN_ACTIVE_CONFLICT(HttpStatus.CONFLICT, "The session already has an active Agent turn."),
    AGENT_TURN_NOT_WAITING_FOR_INPUT(HttpStatus.CONFLICT, "The Agent turn is not waiting for user input."),
    AGENT_TURN_TERMINAL(HttpStatus.CONFLICT, "The Agent turn is already terminal."),
    AGENT_TURN_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "The Agent turn did not finish before the REST timeout."),
    AGENT_DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A required Agent dependency is unavailable."),
    TURN_EXECUTOR_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The Agent turn executor is not available yet."),
    INVALID_EVENT_PAYLOAD(HttpStatus.BAD_REQUEST, "An Agent turn event exceeded the sanitized payload limit.");

    private final HttpStatus status;
    private final String safeMessage;

    TurnErrorCode(HttpStatus status, String safeMessage) {
        this.status = status;
        this.safeMessage = safeMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String safeMessage() {
        return safeMessage;
    }
}

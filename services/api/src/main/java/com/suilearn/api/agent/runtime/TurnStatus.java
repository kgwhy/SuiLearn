package com.suilearn.api.agent.runtime;

public enum TurnStatus {
    CREATED,
    RUNNING,
    WAITING_INPUT,
    COMPLETED,
    CANCELLED,
    FAILED,
    FAILED_ORPHANED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED || this == FAILED_ORPHANED;
    }

    public boolean isActive() {
        return this == RUNNING || this == WAITING_INPUT;
    }
}

package com.suilearn.api.agent.runtime;

public enum EventType {
    TURN_STARTED,
    STAGE_START,
    STAGE_END,
    THINKING,
    CONTENT,
    TOOL_CALL,
    TOOL_RESULT,
    PROGRESS,
    SOURCES,
    RESULT,
    ERROR,
    WAIT_FOR_INPUT,
    DONE,
    CANCELLED,
    FAILED,
    ACTIVE_TURN_INFO;

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED || this == FAILED;
    }

    public String wireName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

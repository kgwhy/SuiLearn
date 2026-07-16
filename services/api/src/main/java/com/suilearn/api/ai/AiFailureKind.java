package com.suilearn.api.ai;

public enum AiFailureKind {
    SUCCESS("success"), TIMEOUT("timeout"), RATE_LIMITED("rate_limited"), TRANSIENT("transient"),
    PERMANENT("permanent"), CIRCUIT_OPEN("circuit_open");

    private final String tagValue;

    AiFailureKind(String tagValue) { this.tagValue = tagValue; }

    public String tagValue() { return tagValue; }
}

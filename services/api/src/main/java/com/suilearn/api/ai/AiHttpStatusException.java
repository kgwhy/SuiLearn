package com.suilearn.api.ai;

public final class AiHttpStatusException extends RuntimeException {
    private final int statusCode;

    public AiHttpStatusException(int statusCode) {
        super("OpenAI-compatible provider returned HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
}

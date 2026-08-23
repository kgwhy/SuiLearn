package com.suilearn.api.agent.runtime;

public class TurnApiException extends RuntimeException {
    private final TurnErrorCode code;

    public TurnApiException(TurnErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public TurnErrorCode code() {
        return code;
    }
}

package com.suilearn.api.agent.runtime;

public class TurnStoreException extends RuntimeException {
    public TurnStoreException(String message) {
        super(message);
    }

    public TurnStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

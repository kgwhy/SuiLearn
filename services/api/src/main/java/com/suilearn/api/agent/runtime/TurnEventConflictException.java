package com.suilearn.api.agent.runtime;

public class TurnEventConflictException extends TurnStoreException {
    public TurnEventConflictException(String turnId, long seq) {
        super("duplicate turn event: turnId=" + turnId + ", seq=" + seq);
    }
}

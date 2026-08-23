package com.suilearn.api.agent.runtime;

public final class TurnEventPayloadException extends TurnApiException {
    public TurnEventPayloadException() {
        super(TurnErrorCode.INVALID_EVENT_PAYLOAD);
    }
}

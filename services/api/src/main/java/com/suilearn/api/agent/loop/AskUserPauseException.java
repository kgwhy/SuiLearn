package com.suilearn.api.agent.loop;

import com.suilearn.api.agent.tool.AskUserPayload;

public final class AskUserPauseException extends RuntimeException {
    private final AskUserPayload payload;

    public AskUserPauseException(AskUserPayload payload) {
        super("pause_for_user");
        this.payload = payload;
    }

    public AskUserPayload payload() {
        return payload;
    }
}

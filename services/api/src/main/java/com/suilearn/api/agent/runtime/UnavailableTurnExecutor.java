package com.suilearn.api.agent.runtime;

import java.util.Map;

/**
 * change-1 placeholder: the runtime lifecycle is real, but no capability executor
 * exists until change-2/change-3. It must never fabricate an answer.
 */
public final class UnavailableTurnExecutor implements TurnExecutor {
    public static final String SOURCE = "turn-runtime";
    public static final String STAGE = "unavailable";
    public static final String ERROR_CODE = "TURN_EXECUTOR_UNAVAILABLE";

    @Override
    public void execute(TurnContext context, TurnEventSink events) {
        events.publish(EventType.PROGRESS, SOURCE, STAGE, "Agent turn executor is bootstrapping", Map.of());
        events.publish(EventType.ERROR, SOURCE, STAGE,
            "This change does not provide a real Agent executor yet.", Map.of("code", ERROR_CODE));
        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, SOURCE, STAGE,
            "Agent turn executor unavailable.", Map.of("code", ERROR_CODE));
    }
}

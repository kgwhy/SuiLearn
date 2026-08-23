package com.suilearn.api.agent.runtime;

import java.util.Map;

/**
 * change-2 turn executor. It resolves the requested capability and emits routing
 * metadata; the real AgentLoop arrives in change-3 and will replace the unavailable
 * terminal state.
 */
public final class TurnOrchestrator implements TurnExecutor {
    public static final String SOURCE = "turn-orchestrator";

    private final CapabilityRegistry capabilities;

    public TurnOrchestrator(CapabilityRegistry capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void execute(TurnContext context, TurnEventSink events) {
        var capability = capabilities.resolve(context);
        String name = capability.manifest().name();
        events.publish(EventType.STAGE_START, name, "routing",
            "Routing turn to capability " + name, Map.of("capability", name));
        events.publish(EventType.PROGRESS, name, "routing",
            "Capability manifest resolved; AgentLoop is not available until change-3.",
            Map.of("capability", name, "ownedTools", capability.manifest().ownedTools()));
        events.publish(EventType.ERROR, name, "unavailable",
            "This change does not provide an LLM AgentLoop yet.", Map.of("code", "TURN_EXECUTOR_UNAVAILABLE"));
        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, name, "unavailable",
            "Agent turn executor unavailable.", Map.of("code", "TURN_EXECUTOR_UNAVAILABLE"));
    }
}

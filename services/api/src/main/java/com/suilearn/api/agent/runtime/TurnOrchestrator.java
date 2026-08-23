package com.suilearn.api.agent.runtime;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.loop.AgentLoop;
import java.util.Map;

/**
 * Routes turn execution by capability. study_agent uses the generic AgentLoop once
 * change-3 wires it; other capabilities remain explicitly unavailable until their
 * loop policy is delivered.
 */
public final class TurnOrchestrator implements TurnExecutor {
    public static final String SOURCE = "turn-orchestrator";

    private final CapabilityRegistry capabilities;
    private final AgentLoop loop;

    public TurnOrchestrator(CapabilityRegistry capabilities) {
        this(capabilities, null);
    }

    public TurnOrchestrator(CapabilityRegistry capabilities, AgentLoop loop) {
        this.capabilities = capabilities;
        this.loop = loop;
    }

    @Override
    public void execute(TurnContext context, TurnEventSink events) {
        var capability = capabilities.resolve(context);
        String name = capability.manifest().name();
        events.publish(EventType.STAGE_START, name, "routing",
            "Routing turn to capability " + name, Map.of("capability", name));
        if (loop != null && BuiltinCapabilities.STUDY_AGENT.equals(name)) {
            loop.run(context, capability.manifest(), events);
            return;
        }
        events.publish(EventType.PROGRESS, name, "routing",
            "Capability manifest resolved; no loop is wired for this capability yet.",
            Map.of("capability", name, "ownedTools", capability.manifest().ownedTools()));
        events.publish(EventType.ERROR, name, "unavailable",
            "This capability does not have an AgentLoop yet.", Map.of("code", "TURN_EXECUTOR_UNAVAILABLE"));
        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, name, "unavailable",
            "Agent turn executor unavailable.", Map.of("code", "TURN_EXECUTOR_UNAVAILABLE"));
    }
}

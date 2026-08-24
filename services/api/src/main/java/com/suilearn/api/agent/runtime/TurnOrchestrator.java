package com.suilearn.api.agent.runtime;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.loop.AgentLoop;
import com.suilearn.api.agent.loop.LoopResult;
import com.suilearn.api.agent.memory.MemoryTurnRecorder;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes turn execution by capability. study_agent, rag_qa, and question_generation
 * share the generic AgentLoop; their differences live in the capability manifest and
 * capability-specific prompt policy. Unknown capabilities remain explicitly unavailable.
 */
public final class TurnOrchestrator implements TurnExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TurnOrchestrator.class);
    public static final String SOURCE = "turn-orchestrator";
    private static final Set<String> LOOP_CAPABILITIES = Set.of(
        BuiltinCapabilities.STUDY_AGENT, BuiltinCapabilities.RAG_QA, BuiltinCapabilities.QUESTION_GENERATION);

    private final CapabilityRegistry capabilities;
    private final AgentLoop loop;
    private final MemoryTurnRecorder memory;

    public TurnOrchestrator(CapabilityRegistry capabilities) {
        this(capabilities, null, null);
    }

    public TurnOrchestrator(CapabilityRegistry capabilities, AgentLoop loop) {
        this(capabilities, loop, null);
    }

    public TurnOrchestrator(CapabilityRegistry capabilities, AgentLoop loop, MemoryTurnRecorder memory) {
        this.capabilities = capabilities;
        this.loop = loop;
        this.memory = memory;
    }

    @Override
    public void execute(TurnContext context, TurnEventSink events) {
        var capability = capabilities.resolve(context);
        String name = capability.manifest().name();
        events.publish(EventType.STAGE_START, name, "routing",
            "Routing turn to capability " + name, Map.of("capability", name));
        if (loop != null && LOOP_CAPABILITIES.contains(name)) {
            LoopResult result = loop.run(context, capability.manifest(), events);
            recordMemory(context, result);
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

    private void recordMemory(TurnContext context, LoopResult result) {
        if (memory == null) {
            return;
        }
        try {
            memory.recordTerminalTurn(context, result.status().name(), result.toolCalls(),
                result.usage(), result.content());
        } catch (RuntimeException failure) {
            // Memory recording is a post-terminal side effect. It must never change a finished turn.
            LOG.warn("Memory turn recording failed for turnId={}", context.turnId(), failure);
        }
    }
}

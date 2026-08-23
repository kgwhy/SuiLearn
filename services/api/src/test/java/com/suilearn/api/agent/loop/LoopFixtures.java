package com.suilearn.api.agent.loop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.InMemoryTurnStore;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnContext;
import com.suilearn.api.agent.runtime.TurnEventBus;
import com.suilearn.api.agent.runtime.TurnEventSink;
import com.suilearn.api.agent.runtime.TurnReplyChannel;
import com.suilearn.api.agent.runtime.ToolRegistry;
import com.suilearn.api.agent.runtime.TurnStore;
import com.suilearn.api.agent.tool.Tool;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LoopFixtures {
    static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");
    static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private LoopFixtures() {}

    static TurnContext context(String capability) {
        return new TurnContext("turn-loop", "sess-loop", "learner-loop", capability,
            new StudyScope("kb-loop", null), List.of(), "Explain hooks", List.of(), List.of(), Map.of());
    }

    static AgentConfigurationProperties properties(int maxSteps, int maxToolCalls) {
        return new AgentConfigurationProperties(true, maxSteps, 3, maxToolCalls, Duration.ofSeconds(10),
            12000, 3, new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }

    static CapabilityManifest studyManifest() {
        return BuiltinCapabilities.studyAgent().manifest();
    }

    static ToolRegistry tools(Map<String, Tool> tools) {
        return new com.suilearn.api.agent.runtime.ToolRegistry(tools);
    }

    static ToolRegistry searchTools(com.suilearn.api.agent.tool.EvidenceSearchPort search) {
        var tools = new LinkedHashMap<String, Tool>();
        var tool = new com.suilearn.api.agent.tool.SearchKnowledgeTool(search);
        tools.put(tool.definition().name(), tool);
        var ask = new com.suilearn.api.agent.tool.AskUserTool();
        tools.put(ask.definition().name(), ask);
        return tools(tools);
    }

    static SinkFixture sink(TurnReplyChannel channel) {
        TurnStore store = new InMemoryTurnStore();
        TurnContext context = context("study_agent");
        var first = new StreamEvent(context.turnId(), context.sessionId(), 1, EventType.TURN_STARTED,
            "study_agent", null, "", Map.of(), NOW);
        store.createTurn(context, "msg-loop", first);
        var bus = new TurnEventBus(context.turnId(), context.sessionId());
        var sink = new TurnEventSink(context.turnId(), context.sessionId(), 1, store, bus, MAPPER,
            Clock.fixed(NOW, ZoneOffset.UTC), channel);
        return new SinkFixture(store, bus, sink);
    }

    record SinkFixture(TurnStore store, TurnEventBus bus, TurnEventSink sink) {}
}

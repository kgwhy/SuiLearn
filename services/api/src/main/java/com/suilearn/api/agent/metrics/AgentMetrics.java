package com.suilearn.api.agent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public final class AgentMetrics {
    private final MeterRegistry registry;

    public AgentMetrics(MeterRegistry registry) { this.registry = registry; }

    public static AgentMetrics noop() { return new AgentMetrics(null); }

    public void recordRun(Outcome outcome, long durationMillis) {
        if (registry == null) return;
        Timer.builder("agent.run").tag("outcome", outcome.tag()).register(registry)
            .record(Duration.ofMillis(Math.max(0, durationMillis)));
    }
    public void recordSubAgent(Agent agent, Outcome outcome) {
        if (registry == null) return;
        Counter.builder("agent.subagent.calls").tags("agent", agent.tag(), "outcome", outcome.tag())
            .register(registry).increment();
    }
    public void recordTool(Tool tool, Outcome outcome) {
        if (registry == null) return;
        Counter.builder("agent.tool.calls").tags("tool", tool.tag(), "outcome", outcome.tag())
            .register(registry).increment();
    }
    public void recordContextTokens(int tokens) {
        if (registry == null) return;
        DistributionSummary.builder("agent.context.tokens").register(registry).record(Math.max(0, tokens));
    }
    public void recordMemory(MemoryLayer layer, Outcome outcome) {
        if (registry == null) return;
        Counter.builder("agent.memory.operations").tags("memoryLayer", layer.tag(), "outcome", outcome.tag())
            .register(registry).increment();
    }

    public enum Outcome {
        SUCCESS("success"), FAILED("failed"), REJECTED("rejected"), BUDGET_EXHAUSTED("budget_exhausted");
        private final String tag; Outcome(String tag) { this.tag = tag; } public String tag() { return tag; }
    }
    public enum Agent {
        KNOWLEDGE_RESEARCH("knowledge_research"), PRACTICE_COACH("practice_coach");
        private final String tag; Agent(String tag) { this.tag = tag; } public String tag() { return tag; }
    }
    public enum Tool {
        KNOWLEDGE_SEARCH("knowledge_search"), EVIDENCE_READ("evidence_read");
        private final String tag; Tool(String tag) { this.tag = tag; } public String tag() { return tag; }
    }
    public enum MemoryLayer {
        SESSION("session"), SEMANTIC("semantic");
        private final String tag; MemoryLayer(String tag) { this.tag = tag; } public String tag() { return tag; }
    }
}

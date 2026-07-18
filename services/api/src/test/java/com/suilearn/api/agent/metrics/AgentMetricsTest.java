package com.suilearn.api.agent.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.agent.tool.*;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import com.suilearn.api.agent.context.*;

class AgentMetricsTest {
    @Test
    void recordsOnlyControlledLowCardinalityTags() {
        var registry = new SimpleMeterRegistry();
        var metrics = new AgentMetrics(registry);

        metrics.recordRun(AgentMetrics.Outcome.SUCCESS, 12);
        metrics.recordSubAgent(AgentMetrics.Agent.KNOWLEDGE_RESEARCH, AgentMetrics.Outcome.SUCCESS);
        metrics.recordTool(AgentMetrics.Tool.KNOWLEDGE_SEARCH, AgentMetrics.Outcome.REJECTED);
        metrics.recordContextTokens(1200);
        metrics.recordMemory(AgentMetrics.MemoryLayer.SESSION, AgentMetrics.Outcome.SUCCESS);

        assertThat(registry.find("agent.run").tag("outcome", "success").timer().count()).isEqualTo(1);
        assertThat(registry.find("agent.subagent.calls").tag("agent", "knowledge_research").counter().count())
            .isEqualTo(1);
        assertThat(registry.find("agent.tool.calls").tag("tool", "knowledge_search").counter().count())
            .isEqualTo(1);
        registry.getMeters().forEach(meter -> assertThat(meter.getId().getTags())
            .allSatisfy(tag -> {
                assertThat(tag.getKey()).isIn("outcome", "agent", "tool", "memoryLayer");
                assertThat(tag.getValue()).doesNotContain(
                    "learner-123", "session-456", "run-789", "prompt-body", "question-body");
            }));
    }

    @Test
    void productionSubagentAndToolOperationsRecordControlledMetrics() {
        var registry = new SimpleMeterRegistry();
        var metrics = new AgentMetrics(registry);
        var pointer = new EvidencePointer("e", "source", "kb", "material", 1.0);
        var research = new KnowledgeResearchSubAgent(request -> List.of(pointer),
            request -> Optional.of(new EvidenceRecord("e", "source", "kb", "material", "body", false)),
            AgentToolCatalog.fixedMvp(), metrics);

        research.research(new KnowledgeResearchSubAgent.Request("goal", new AgentScope("kb", null), 1),
            new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(90), Clock.systemUTC()));

        assertThat(registry.find("agent.subagent.calls").tag("agent", "knowledge_research").counter().count())
            .isEqualTo(1);
        assertThat(registry.find("agent.tool.calls").tag("tool", "knowledge_search").counter().count())
            .isEqualTo(1);
        assertThat(registry.find("agent.tool.calls").tag("tool", "evidence_read").counter().count())
            .isEqualTo(1);

        new ContextAssembler(String::length, new ContextBudgetPolicy(), metrics).assemble(
            new AgentContextRequest("system", "task", "scope", List.of(), List.of(), List.of(), List.of()), 100);
        assertThat(registry.find("agent.context.tokens").summary().count()).isEqualTo(1);
    }
}

package com.suilearn.api.agent.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.tool.AgentAction;
import com.suilearn.api.agent.tool.AgentRole;
import com.suilearn.api.agent.tool.SharedAgentBudget;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AgentBudgetTest {
    @Test
    void enforcesSupervisorSubagentAndSharedToolLimits() {
        var supervisor = new SharedAgentBudget(1, 1, 2, Duration.ofSeconds(90), Clock.systemUTC());
        supervisor.consumeStep(AgentRole.SUPERVISOR);
        assertThatThrownBy(() -> supervisor.consumeStep(AgentRole.SUPERVISOR))
            .isInstanceOf(SharedAgentBudget.BudgetExhaustedException.class)
            .hasMessage("BUDGET_EXHAUSTED");

        var subagent = new SharedAgentBudget(4, 1, 2, Duration.ofSeconds(90), Clock.systemUTC());
        subagent.consumeStep(AgentRole.KNOWLEDGE_RESEARCH);
        assertThatThrownBy(() -> subagent.consumeStep(AgentRole.KNOWLEDGE_RESEARCH))
            .isInstanceOf(SharedAgentBudget.BudgetExhaustedException.class);

        var tools = new SharedAgentBudget(4, 3, 2, Duration.ofSeconds(90), Clock.systemUTC());
        tools.consumeTool(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.SEARCH_KNOWLEDGE);
        tools.consumeTool(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.READ_EVIDENCE);
        assertThatThrownBy(() -> tools.consumeTool(AgentRole.SUPERVISOR, AgentAction.PRACTICE_COACH))
            .isInstanceOf(SharedAgentBudget.BudgetExhaustedException.class);
        assertThat(tools.usage().toolCalls()).isEqualTo(2);
    }

    @Test
    void enforcesTimeoutWithoutLeakingTaskContent() {
        var clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        var budget = new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(10), clock);
        clock.advance(Duration.ofSeconds(11));

        assertThatThrownBy(() -> budget.consumeStep(AgentRole.SUPERVISOR))
            .isInstanceOf(SharedAgentBudget.BudgetExhaustedException.class)
            .hasMessage("BUDGET_EXHAUSTED")
            .hasMessageNotContaining("learner")
            .hasMessageNotContaining("question")
            .hasMessageNotContaining("evidence");
        assertThat(budget.usage().timedOut()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}

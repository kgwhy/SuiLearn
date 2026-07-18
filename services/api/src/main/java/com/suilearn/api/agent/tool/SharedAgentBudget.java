package com.suilearn.api.agent.tool;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SharedAgentBudget {
    private final int supervisorMaxSteps;
    private final int subagentMaxSteps;
    private final int maxToolCalls;
    private final Clock clock;
    private final Instant deadline;
    private final AgentToolCatalog catalog = AgentToolCatalog.fixedMvp();
    private final Map<AgentRole, Integer> steps = new EnumMap<>(AgentRole.class);
    private int toolCalls;
    private boolean timedOut;

    public SharedAgentBudget(int supervisorMaxSteps, int subagentMaxSteps, int maxToolCalls,
                             Duration timeout, Clock clock) {
        if (supervisorMaxSteps < 1 || subagentMaxSteps < 1 || maxToolCalls < 1) {
            throw new IllegalArgumentException("budget limits must be positive");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.supervisorMaxSteps = supervisorMaxSteps;
        this.subagentMaxSteps = subagentMaxSteps;
        this.maxToolCalls = maxToolCalls;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.deadline = clock.instant().plus(timeout);
    }

    public synchronized void consumeStep(AgentRole role) {
        ensureTime();
        int limit = role == AgentRole.SUPERVISOR ? supervisorMaxSteps : subagentMaxSteps;
        int used = steps.getOrDefault(role, 0);
        if (used >= limit) {
            throw new BudgetExhaustedException();
        }
        steps.put(role, used + 1);
    }

    public synchronized void consumeTool(AgentRole role, AgentAction action) {
        ensureTime();
        catalog.requireAllowed(role, action);
        if (toolCalls >= maxToolCalls) {
            throw new BudgetExhaustedException();
        }
        toolCalls++;
    }

    public synchronized Usage usage() {
        int supervisorSteps = steps.getOrDefault(AgentRole.SUPERVISOR, 0);
        int subagentSteps = steps.getOrDefault(AgentRole.KNOWLEDGE_RESEARCH, 0)
            + steps.getOrDefault(AgentRole.PRACTICE_COACH, 0);
        return new Usage(supervisorSteps, subagentSteps, toolCalls, timedOut);
    }

    public synchronized void checkTime() {
        ensureTime();
    }

    public synchronized void markTimedOut() {
        timedOut = true;
    }

    private void ensureTime() {
        if (!clock.instant().isBefore(deadline)) {
            timedOut = true;
            throw new BudgetExhaustedException();
        }
    }

    public record Usage(int supervisorSteps, int subagentSteps, int toolCalls, boolean timedOut) {
    }

    public static final class BudgetExhaustedException extends IllegalStateException {
        public BudgetExhaustedException() {
            super("BUDGET_EXHAUSTED");
        }
    }
}

package com.suilearn.api.agent.application;

import java.util.List;
import java.util.Objects;

@FunctionalInterface
public interface LearningAgentPort {
    StudyRunResult run(StudyRunRequest request);

    record StudyRunRequest(
        String learnerId,
        String sessionId,
        String question,
        AgentScope scope,
        int practiceCount,
        Difficulty difficulty
    ) {
        public StudyRunRequest {
            learnerId = requireText(learnerId, "learnerId");
            if (sessionId != null && sessionId.isBlank()) {
                throw new IllegalArgumentException("sessionId must be absent or non-blank");
            }
            question = requireText(question, "question");
            Objects.requireNonNull(scope, "scope");
            if (practiceCount < 1 || practiceCount > 5) {
                throw new IllegalArgumentException("practiceCount must be between 1 and 5");
            }
            Objects.requireNonNull(difficulty, "difficulty");
        }
    }

    record AgentScope(String knowledgeBaseId, String materialId) {
        public AgentScope {
            knowledgeBaseId = normalize(knowledgeBaseId);
            materialId = normalize(materialId);
            if (knowledgeBaseId == null && materialId == null) {
                throw new IllegalArgumentException("knowledgeBaseId or materialId is required");
            }
        }

        public boolean matches(String candidateKnowledgeBaseId, String candidateMaterialId) {
            return (knowledgeBaseId == null || knowledgeBaseId.equals(candidateKnowledgeBaseId))
                && (materialId == null || materialId.equals(candidateMaterialId));
        }

        private static String normalize(String value) {
            return value == null || value.isBlank() ? null : value.strip();
        }
    }

    record StudyRunResult(
        String runId,
        String sessionId,
        String answer,
        boolean uncertain,
        List<Citation> citations,
        List<Exercise> exercises,
        String nextStep,
        RunStatus status,
        BudgetUsage budgetUsage,
        List<ActionTrace> actionTrace,
        MemoryStatus memory,
        List<DegradationStatus> degradationStatuses
    ) {
        public StudyRunResult {
            runId = requireText(runId, "runId");
            sessionId = requireText(sessionId, "sessionId");
            answer = Objects.requireNonNull(answer, "answer");
            citations = List.copyOf(citations == null ? List.of() : citations);
            exercises = List.copyOf(exercises == null ? List.of() : exercises);
            nextStep = nextStep == null ? "" : nextStep;
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(budgetUsage, "budgetUsage");
            actionTrace = List.copyOf(actionTrace == null ? List.of() : actionTrace);
            memory = memory == null ? MemoryStatus.notAttempted() : memory;
            degradationStatuses = List.copyOf(degradationStatuses == null ? List.of() : degradationStatuses);
            if (uncertain && (!citations.isEmpty() || !exercises.isEmpty())) {
                throw new IllegalArgumentException("uncertain result cannot claim citations or exercises");
            }
        }

        public StudyRunResult(String runId, String sessionId, String answer, boolean uncertain,
                              List<Citation> citations, List<Exercise> exercises, String nextStep,
                              RunStatus status, BudgetUsage budgetUsage, List<ActionTrace> actionTrace) {
            this(runId, sessionId, answer, uncertain, citations, exercises, nextStep, status, budgetUsage,
                actionTrace, MemoryStatus.notAttempted(), List.of());
        }

        public static StudyRunResult noEvidence(String runId, String sessionId, BudgetUsage budgetUsage) {
            return new StudyRunResult(runId, sessionId, "", true, List.of(), List.of(), "",
                RunStatus.NO_EVIDENCE, budgetUsage, List.of());
        }
    }

    record Citation(String stableId, String sourceRef, String materialId, String revisionId,
                    Integer pageNumber, String blockId, String excerpt, boolean deleted) {
        public Citation {
            stableId = requireText(stableId, "stableId");
            sourceRef = requireText(sourceRef, "sourceRef");
            materialId = requireText(materialId, "materialId");
            revisionId = requireText(revisionId, "revisionId");
            excerpt = requireText(excerpt, "excerpt");
            blockId = blockId == null || blockId.isBlank() ? null : blockId.strip();
            if ((pageNumber == null || pageNumber < 1) && blockId == null) {
                throw new IllegalArgumentException("pageNumber or blockId is required");
            }
        }
    }

    record Exercise(String id, ExerciseType type, String question, List<String> options,
                    String answer, String explanation, List<Citation> citations) {
        public Exercise {
            id = requireText(id, "id");
            Objects.requireNonNull(type, "type");
            question = requireText(question, "question");
            options = List.copyOf(options == null ? List.of() : options);
            answer = requireText(answer, "answer");
            explanation = requireText(explanation, "explanation");
            citations = List.copyOf(citations == null ? List.of() : citations);
        }
    }

    record BudgetUsage(int supervisorSteps, int maxSupervisorSteps, int subagentSteps,
                       int maxSubagentStepsPerCall, int toolCalls, int maxToolCalls,
                       long elapsedMillis, long timeoutMillis, boolean timedOut) {
        public BudgetUsage {
            if (supervisorSteps < 0 || subagentSteps < 0 || toolCalls < 0 || elapsedMillis < 0
                || maxSupervisorSteps < 1 || maxSubagentStepsPerCall < 1 || maxToolCalls < 1 || timeoutMillis < 1) {
                throw new IllegalArgumentException("budget usage cannot be negative");
            }
        }

        public BudgetUsage(int supervisorSteps, int subagentSteps, int toolCalls, boolean timedOut) {
            this(supervisorSteps, Math.max(1, supervisorSteps), subagentSteps, Math.max(1, subagentSteps),
                toolCalls, Math.max(1, toolCalls), 0, 1, timedOut);
        }
    }

    record MemoryStatus(SessionMemoryStatus session, SemanticRecallStatus semanticRecall,
                        SemanticPersistenceStatus semanticPersistence) {
        public MemoryStatus {
            Objects.requireNonNull(session, "session");
            Objects.requireNonNull(semanticRecall, "semanticRecall");
            Objects.requireNonNull(semanticPersistence, "semanticPersistence");
        }

        public static MemoryStatus notAttempted() {
            return new MemoryStatus(SessionMemoryStatus.AVAILABLE, SemanticRecallStatus.EMPTY,
                SemanticPersistenceStatus.NOT_ATTEMPTED);
        }
    }

    record ActionTrace(int step, TraceActor actor, TraceAction action, TraceStatus status, long durationMillis) {
        public ActionTrace {
            if (step < 1 || durationMillis < 0) {
                throw new IllegalArgumentException("invalid action trace metadata");
            }
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(status, "status");
        }
    }

    enum Difficulty { EASY, MEDIUM, HARD }
    enum ExerciseType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER }
    enum RunStatus { COMPLETED, NO_EVIDENCE, BUDGET_EXHAUSTED, INVALID_MODEL_OUTPUT }
    enum SessionMemoryStatus { AVAILABLE, UPDATED, UNAVAILABLE }
    enum SemanticRecallStatus { AVAILABLE, EMPTY, LONG_TERM_MEMORY_DEGRADED }
    enum SemanticPersistenceStatus { NOT_ATTEMPTED, PERSISTED, NO_CANDIDATE, PERSIST_FAILED }
    enum DegradationStatus { LONG_TERM_MEMORY_DEGRADED, MEMORY_PERSIST_FAILED }
    enum TraceActor { SUPERVISOR, KNOWLEDGE_RESEARCH, PRACTICE_COACH }
    enum TraceAction { KNOWLEDGE_RESEARCH, PRACTICE_COACH, SEARCH_KNOWLEDGE, READ_EVIDENCE, SCHEMA_REPAIR, STOP }
    enum TraceStatus { COMPLETED, REJECTED, EXHAUSTED, FAILED }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}

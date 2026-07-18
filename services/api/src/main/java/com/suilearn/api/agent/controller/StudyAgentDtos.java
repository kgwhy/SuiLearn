package com.suilearn.api.agent.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class StudyAgentDtos {
    private StudyAgentDtos() {}

    public record RunRequest(
        @NotBlank @Size(max = 128) String learnerId,
        @Size(min = 1, max = 128) String sessionId,
        @NotBlank String question,
        @Size(min = 1) String knowledgeBaseId,
        @Size(min = 1) String materialId,
        @Min(1) @Max(5) Integer practiceCount,
        Difficulty difficulty
    ) {
        public RunRequest(String learnerId, String sessionId, String question, String knowledgeBaseId,
                          String materialId, Integer practiceCount) {
            this(learnerId, sessionId, question, knowledgeBaseId, materialId, practiceCount, null);
        }

        public int effectivePracticeCount(int defaultCount) {
            return practiceCount == null ? defaultCount : practiceCount;
        }

        public Difficulty effectiveDifficulty() {
            return difficulty == null ? Difficulty.MEDIUM : difficulty;
        }
    }

    public record RunResponse(
        String runId, String sessionId, RunStatus status, String answer, boolean uncertain,
        List<SourceCitation> citations, List<PracticeItem> practiceItems, List<String> nextSteps,
        MemoryStatus memory, BudgetUsage budgetUsage, List<ActionTraceEntry> actionTrace,
        List<DegradationStatus> degradationStatuses
    ) {
        public RunResponse {
            citations = List.copyOf(citations); practiceItems = List.copyOf(practiceItems);
            nextSteps = List.copyOf(nextSteps); actionTrace = List.copyOf(actionTrace);
            degradationStatuses = List.copyOf(degradationStatuses);
        }
    }

    public record SourceCitation(String materialId, String revisionId, Integer pageNumber,
                                 String blockId, String excerpt, boolean deleted) {}
    public record PracticeItem(String id, PracticeType type, String question, List<String> options,
                               String answer, String explanation, List<SourceCitation> citations) {}
    public record MemoryStatus(SessionStatus session, SemanticRecallStatus semanticRecall,
                               SemanticPersistenceStatus semanticPersistence) {}
    public record BudgetUsage(int supervisorStepsUsed, int maxSupervisorSteps, int subAgentStepsUsed,
                              int maxSubAgentStepsPerCall, int toolCallsUsed, int maxToolCalls,
                              long elapsedMs, long timeoutMs) {}
    public record ActionTraceEntry(int sequence, ActorType actorType, TraceActor actor,
                                   TraceAction action, TraceStatus status, long durationMs) {}
    public record MemoryDeletionResponse(String learnerId, MemoryLayerDeletion session,
                                         MemoryLayerDeletion semantic) {}
    public record MemoryLayerDeletion(DeletionResult status, long deletedCount) {}
    public record AgentError(AgentErrorCode code, String message, String correlationId,
                             List<FieldError> fieldErrors) {
        public AgentError { fieldErrors = List.copyOf(fieldErrors == null ? List.of() : fieldErrors); }
    }
    public record FieldError(String field, String code, String message) {}

    public enum RunStatus { COMPLETED, UNCERTAIN, BUDGET_EXHAUSTED }
    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum PracticeType { SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER }
    public enum SessionStatus { AVAILABLE, UPDATED, UNAVAILABLE }
    public enum SemanticRecallStatus { AVAILABLE, EMPTY, LONG_TERM_MEMORY_DEGRADED }
    public enum SemanticPersistenceStatus { NOT_ATTEMPTED, PERSISTED, NO_CANDIDATE, PERSIST_FAILED }
    public enum ActorType { SUPERVISOR, SUBAGENT, TOOL, RUNTIME }
    public enum TraceActor { STUDY_SUPERVISOR, KNOWLEDGE_RESEARCH, PRACTICE_COACH, KNOWLEDGE_SEARCH,
        EVIDENCE_READ, MEMORY, SCHEMA_VALIDATOR, BUDGET }
    public enum TraceAction { START, DELEGATE, SEARCH, READ, GENERATE_PRACTICE, RECALL_MEMORY,
        PERSIST_MEMORY, SCHEMA_REPAIR, STOP }
    public enum TraceStatus { SUCCEEDED, FAILED, REJECTED, SKIPPED, BUDGET_EXHAUSTED }
    public enum DegradationStatus { LONG_TERM_MEMORY_DEGRADED, MEMORY_PERSIST_FAILED }
    public enum DeletionResult { DELETED, NOT_FOUND, FAILED }
}

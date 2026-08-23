package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.metrics.AgentMetrics;
import java.util.HashSet;
import java.util.Set;

/**
 * Deterministic practice-validation helper used only by GeneratePracticeTool.
 * It is not an Agent graph and does not consume a legacy catalog or budget.
 */
public final class PracticeCoachSubAgent {
    private final PracticeModelPort model;
    private final AgentMetrics metrics;

    public PracticeCoachSubAgent(PracticeModelPort model) {
        this(model, AgentMetrics.noop());
    }

    public PracticeCoachSubAgent(PracticeModelPort model, AgentMetrics metrics) {
        this.model = model;
        this.metrics = metrics;
    }

    public PracticeResult coach(Request request) {
        if (request.evidence().items().isEmpty()) {
            metrics.recordSubAgent(AgentMetrics.Agent.PRACTICE_COACH, AgentMetrics.Outcome.REJECTED);
            return PracticeResult.noEvidence();
        }
        if (request.evidence().items().stream().anyMatch(item -> !item.verified() || !item.untrusted())) {
            throw new InvalidEvidenceException();
        }
        PracticeModelPort.Draft draft;
        try {
            draft = model.generate(new PracticeModelPort.Request(
                request.learningGoal(), request.evidence(), request.difficulty(), request.practiceCount()));
        } catch (RuntimeException exception) {
            metrics.recordSubAgent(AgentMetrics.Agent.PRACTICE_COACH, AgentMetrics.Outcome.FAILED);
            throw exception;
        }
        Set<String> allowedRefs = request.evidence().items().stream()
            .map(EvidenceBundle.Item::sourceRef)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (draft.citations().isEmpty()) {
            throw new InvalidEvidenceException();
        }
        validateCitations(draft.citations(), allowedRefs);
        draft.exercises().forEach(exercise -> validateCitations(exercise.citations(), allowedRefs));
        if (draft.exercises().size() > request.practiceCount()) {
            throw new IllegalArgumentException("PRACTICE_COUNT_EXCEEDED");
        }
        metrics.recordSubAgent(AgentMetrics.Agent.PRACTICE_COACH, AgentMetrics.Outcome.SUCCESS);
        return new PracticeResult(draft.explanation(), draft.exercises(), draft.citations(), draft.nextStep(), false);
    }

    private void validateCitations(java.util.List<String> citations, Set<String> allowedRefs) {
        if (new HashSet<>(citations).size() != citations.size() || !allowedRefs.containsAll(citations)) {
            throw new InvalidEvidenceException();
        }
    }

    public record Request(String learningGoal, EvidenceBundle evidence, PracticeDifficulty difficulty, int practiceCount) {
        public Request {
            learningGoal = RequiredText.value(learningGoal, "learningGoal");
            if (evidence == null || difficulty == null) {
                throw new IllegalArgumentException("evidence and difficulty are required");
            }
            if (practiceCount < 1 || practiceCount > 5) {
                throw new IllegalArgumentException("practiceCount must be between 1 and 5");
            }
        }
    }
}

package com.suilearn.api.agent.context;

import java.util.List;
import java.util.Objects;

public final class ContextManager {
    private final ContextAssembler assembler;
    private final int maximumTokens;

    public ContextManager(ContextAssembler assembler, int maximumTokens) {
        this.assembler = Objects.requireNonNull(assembler, "assembler");
        if (maximumTokens < 1) {
            throw new IllegalArgumentException("maximumTokens must be positive");
        }
        this.maximumTokens = maximumTokens;
    }

    public AgentContextSnapshot assemble(AgentContextRequest request) {
        return assembler.assemble(request, maximumTokens);
    }

    public KnowledgeResearchSnapshot forKnowledgeResearch(
        String researchGoal,
        String scope,
        List<String> necessaryLearningMemories,
        int evidenceLimit,
        int toolCallBudget
    ) {
        if (evidenceLimit < 1 || toolCallBudget < 1) {
            throw new IllegalArgumentException("evidenceLimit and toolCallBudget must be positive");
        }
        return new KnowledgeResearchSnapshot(
            requireText(researchGoal, "researchGoal"), requireText(scope, "scope"),
            List.copyOf(necessaryLearningMemories == null ? List.of() : necessaryLearningMemories),
            evidenceLimit, toolCallBudget, true);
    }

    public PracticeCoachSnapshot forPracticeCoach(
        String learningGoal,
        List<EvidenceItem> evidence,
        String difficulty,
        int practiceCount,
        String outputSchema
    ) {
        if (practiceCount < 1 || practiceCount > 5) {
            throw new IllegalArgumentException("practiceCount must be between 1 and 5");
        }
        List<EvidenceItem> verified = (evidence == null ? List.<EvidenceItem>of() : evidence).stream()
            .filter(EvidenceItem::verified)
            .toList();
        if (verified.size() != (evidence == null ? 0 : evidence.size())) {
            throw new IllegalArgumentException("practice context accepts verified evidence only");
        }
        return new PracticeCoachSnapshot(
            requireText(learningGoal, "learningGoal"), verified, requireText(difficulty, "difficulty"),
            practiceCount, requireText(outputSchema, "outputSchema"));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    public record KnowledgeResearchSnapshot(
        String researchGoal,
        String scope,
        List<String> necessaryLearningMemories,
        int evidenceLimit,
        int toolCallBudget,
        boolean evidenceIsUntrustedData
    ) {
        public KnowledgeResearchSnapshot {
            necessaryLearningMemories = List.copyOf(necessaryLearningMemories);
        }
    }

    public record PracticeCoachSnapshot(
        String learningGoal,
        List<EvidenceItem> verifiedEvidence,
        String difficulty,
        int practiceCount,
        String outputSchema
    ) {
        public PracticeCoachSnapshot {
            verifiedEvidence = List.copyOf(verifiedEvidence);
        }
    }
}

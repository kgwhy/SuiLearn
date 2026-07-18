package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryPromotionPolicyTest {
    private final MemoryPromotionPolicy policy = new MemoryPromotionPolicy(0.80, 8, 500);

    @Test
    void acceptsOnlyAllowedSourcedCandidatesWithMatchingLearnerAndFingerprint() {
        String content = "Needs more practice with React hooks";
        String fingerprint = MemoryFingerprint.of(content);
        MemoryCandidate valid = candidate("learner-a", MemoryType.WEAKNESS, content, fingerprint, 0.80, "run-1", "evidence-1");

        assertThat(policy.evaluate("learner-a", valid).accepted()).isTrue();
        assertThat(policy.evaluate("other", valid).reason()).isEqualTo(PromotionRejection.LEARNER_MISMATCH);
        assertThat(policy.evaluate("learner-a", candidate("learner-a", MemoryType.WEAKNESS, content, "wrong", 0.9, "run-1", "evidence-1")).reason())
            .isEqualTo(PromotionRejection.FINGERPRINT_MISMATCH);
        assertThat(policy.evaluate("learner-a", candidate("learner-a", MemoryType.GOAL, content, fingerprint, 0.79, "run-1", "evidence-1")).reason())
            .isEqualTo(PromotionRejection.LOW_CONFIDENCE);
        assertThat(policy.evaluate("learner-a", candidate("learner-a", MemoryType.PREFERENCE, content, fingerprint, 0.9, "", "evidence-1")).reason())
            .isEqualTo(PromotionRejection.MISSING_SOURCE);
        assertThat(policy.evaluate("learner-a", candidate("learner-a", MemoryType.MASTERY, "short", MemoryFingerprint.of("short"), 0.9, "run-1", "evidence-1")).reason())
            .isEqualTo(PromotionRejection.INVALID_LENGTH);
    }

    @Test
    void reportsNoCandidateWithoutCallingSemanticPersistence() {
        InMemorySemanticMemoryStore store = new InMemorySemanticMemoryStore();
        MemoryManager manager = manager(store, query -> EmbeddingResult.available(List.of(1.0, 0.0)));

        assertThat(manager.promote("learner-a", null).status()).isEqualTo(PersistenceStatus.NO_CANDIDATE);
        assertThat(store.findByLearnerAndTypes("learner-a", MemoryType.allowed())).isEmpty();
    }

    @Test
    void deduplicatesFingerprintsAndResolvesMasteryConflictsDeterministically() {
        InMemorySemanticMemoryStore store = new InMemorySemanticMemoryStore();
        MemoryManager manager = manager(store, query -> EmbeddingResult.available(List.of(1.0, 0.0)));
        String first = "React hooks mastery is developing";

        assertThat(manager.promote("learner-a", candidate("learner-a", MemoryType.MASTERY, first,
            MemoryFingerprint.of(first), 0.85, "run-1", "topic:hooks")).status()).isEqualTo(PersistenceStatus.PERSISTED);
        assertThat(manager.promote("learner-a", candidate("learner-a", MemoryType.MASTERY, "  react HOOKS mastery is developing ",
            MemoryFingerprint.of(first), 0.90, "run-2", "topic:hooks")).status()).isEqualTo(PersistenceStatus.PERSISTED);
        assertThat(store.findByLearnerAndTypes("learner-a", MemoryType.allowed())).hasSize(1);

        manager.promote("learner-a", candidate("learner-a", MemoryType.MASTERY, first,
            MemoryFingerprint.of(first), 0.86, "run-low", "topic:hooks"));
        assertThat(store.findByLearnerAndTypes("learner-a", MemoryType.allowed())).singleElement()
            .satisfies(memory -> {
                assertThat(memory.confidence()).isEqualTo(0.90);
                assertThat(memory.sourceRunId()).isEqualTo("run-2");
            });

        String improved = "React hooks mastery is proficient";
        manager.promote("learner-a", candidate("learner-a", MemoryType.MASTERY, improved,
            MemoryFingerprint.of(improved), 0.95, "run-3", "topic:hooks"));
        assertThat(store.findByLearnerAndTypes("learner-a", MemoryType.allowed())).singleElement()
            .satisfies(memory -> {
                assertThat(memory.content()).isEqualTo(improved);
                assertThat(memory.sourceRunId()).isEqualTo("run-3");
            });

        String lowerConfidenceConflict = "React hooks mastery is weak";
        manager.promote("learner-a", candidate("learner-a", MemoryType.MASTERY, lowerConfidenceConflict,
            MemoryFingerprint.of(lowerConfidenceConflict), 0.81, "run-4", "topic:hooks"));
        assertThat(store.findByLearnerAndTypes("learner-a", MemoryType.allowed())).singleElement()
            .extracting(AgentSemanticMemory::content).isEqualTo(improved);
    }

    private MemoryManager manager(SemanticMemoryStore store, EmbeddingProvider embedding) {
        return new MemoryManager(null, store, embedding, policy, 5, Instant::now);
    }

    private MemoryCandidate candidate(String learner, MemoryType type, String content, String fingerprint,
                                      double confidence, String run, String source) {
        return new MemoryCandidate(learner, type, content, fingerprint, confidence, run, source);
    }
}

package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SemanticAgentMemoryTest {
    @Test
    void filtersLearnerAndTypesBeforeSimilarityAndAppliesTopK() {
        InMemorySemanticMemoryStore store = new InMemorySemanticMemoryStore();
        store.save(memory("a-goal", "learner-a", MemoryType.GOAL, List.of(1.0, 0.0)));
        store.save(memory("a-pref", "learner-a", MemoryType.PREFERENCE, List.of(0.9, 0.1)));
        store.save(memory("a-weak", "learner-a", MemoryType.WEAKNESS, List.of(0.99, 0.01)));
        store.save(memory("b-goal", "learner-b", MemoryType.GOAL, List.of(1.0, 0.0)));
        MemoryManager manager = manager(store, query -> EmbeddingResult.available(List.of(1.0, 0.0)), 1);

        SemanticRecallResult recalled = manager.recall("learner-a", Set.of(MemoryType.GOAL, MemoryType.PREFERENCE), "react");

        assertThat(recalled.status()).isEqualTo(RecallStatus.AVAILABLE);
        assertThat(recalled.memories()).extracting(ScoredSemanticMemory::memory).extracting(AgentSemanticMemory::id)
            .containsExactly("a-goal");
        assertThat(store.lastQuery()).satisfies(query -> {
            assertThat(query.learnerId()).isEqualTo("learner-a");
            assertThat(query.types()).containsExactlyInAnyOrder(MemoryType.GOAL, MemoryType.PREFERENCE);
        });
    }

    @Test
    void returnsExplicitDegradedStatusAndNoArbitraryRecordsWithoutEmbedding() {
        InMemorySemanticMemoryStore store = new InMemorySemanticMemoryStore();
        store.save(memory("a-goal", "learner-a", MemoryType.GOAL, List.of(1.0, 0.0)));
        MemoryManager manager = manager(store, query -> EmbeddingResult.unavailable("model unavailable"), 5);

        SemanticRecallResult recalled = manager.recall("learner-a", Set.of(MemoryType.GOAL), "react");

        assertThat(recalled.status()).isEqualTo(RecallStatus.LONG_TERM_MEMORY_DEGRADED);
        assertThat(recalled.memories()).isEmpty();
        assertThat(store.lastQuery()).isNull();
    }

    @Test
    void reportsHonestDeleteCountsAndFailuresWithoutTouchingOtherLearners() {
        InMemorySessionMemoryStore sessions = new InMemorySessionMemoryStore(java.time.Clock.systemUTC());
        SessionMemoryService sessionService = new SessionMemoryService(
            sessions, new SessionMemoryKeyFactory("suilearn:agent:session:v1"), java.time.Duration.ofHours(4), 20);
        sessionService.append("learner-a", "s1", new SessionTurn("a", null, Instant.now()));
        sessionService.append("learner-b", "s2", new SessionTurn("b", null, Instant.now()));
        InMemorySemanticMemoryStore semantic = new InMemorySemanticMemoryStore();
        semantic.save(memory("a-goal", "learner-a", MemoryType.GOAL, List.of(1.0, 0.0)));
        semantic.save(memory("b-goal", "learner-b", MemoryType.GOAL, List.of(1.0, 0.0)));
        MemoryManager manager = new MemoryManager(sessionService, semantic,
            query -> EmbeddingResult.available(List.of(1.0, 0.0)), new MemoryPromotionPolicy(0.8, 8, 500), 5, Instant::now);

        MemoryDeletionResult deleted = manager.deleteLearnerMemory("learner-a");
        assertThat(deleted.session()).isEqualTo(LayerDeletion.succeeded(1));
        assertThat(deleted.semantic()).isEqualTo(LayerDeletion.succeeded(1));
        assertThat(sessionService.read("learner-b", "s2")).isPresent();
        assertThat(semantic.findByLearnerAndTypes("learner-b", MemoryType.allowed())).hasSize(1);

        semantic.failDeletes();
        MemoryDeletionResult failed = manager.deleteLearnerMemory("learner-b");
        assertThat(failed.semantic().status()).isEqualTo(DeletionStatus.FAILED);
        assertThat(failed.semantic().deletedCount()).isZero();
    }

    private MemoryManager manager(SemanticMemoryStore store, EmbeddingProvider embedding, int topK) {
        return new MemoryManager(null, store, embedding, new MemoryPromotionPolicy(0.8, 8, 500), topK, Instant::now);
    }

    private AgentSemanticMemory memory(String id, String learner, MemoryType type, List<Double> embedding) {
        return new AgentSemanticMemory(id, learner, type, id, MemoryFingerprint.of(id), embedding, 0.9,
            "run", "topic:" + id, Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));
    }
}

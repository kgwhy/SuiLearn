package com.suilearn.api.agent.memory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class MemoryManager {
    private final SessionMemoryService sessions;
    private final SemanticMemoryStore semanticStore;
    private final EmbeddingProvider embeddings;
    private final MemoryPromotionPolicy promotionPolicy;
    private final int topK;
    private final Supplier<Instant> now;

    public MemoryManager(SessionMemoryService sessions, SemanticMemoryStore semanticStore,
                         EmbeddingProvider embeddings, MemoryPromotionPolicy promotionPolicy,
                         int topK, Supplier<Instant> now) {
        this.sessions = sessions;
        this.semanticStore = Objects.requireNonNull(semanticStore, "semanticStore");
        this.embeddings = Objects.requireNonNull(embeddings, "embeddings");
        this.promotionPolicy = Objects.requireNonNull(promotionPolicy, "promotionPolicy");
        if (topK < 1) {
            throw new IllegalArgumentException("topK must be positive");
        }
        this.topK = topK;
        this.now = Objects.requireNonNull(now, "now");
    }

    public WorkingMemory openWorkingMemory() {
        return new WorkingMemory();
    }

    public Optional<SessionMemory> readSession(String learnerId, String sessionId) {
        if (sessions == null) {
            throw new IllegalStateException("session memory is unavailable");
        }
        return sessions.read(learnerId, sessionId);
    }

    public void appendSession(String learnerId, String sessionId, SessionTurn turn) {
        if (sessions == null) {
            throw new IllegalStateException("session memory is unavailable");
        }
        sessions.append(learnerId, sessionId, turn);
    }

    public SemanticRecallResult recall(String learnerId, Set<MemoryType> requestedTypes, String query) {
        requireText(learnerId, "learnerId");
        requireText(query, "query");
        Set<MemoryType> types = requestedTypes == null ? Set.of() : Set.copyOf(requestedTypes);
        if (types.isEmpty() || !MemoryType.allowed().containsAll(types)) {
            throw new IllegalArgumentException("at least one allowed memory type is required");
        }
        EmbeddingResult queryEmbedding;
        try {
            queryEmbedding = embeddings.embed(query);
        } catch (RuntimeException unavailable) {
            return SemanticRecallResult.degraded("embedding unavailable");
        }
        if (queryEmbedding == null || !queryEmbedding.available()) {
            return SemanticRecallResult.degraded(queryEmbedding == null ? "embedding unavailable" : queryEmbedding.reason());
        }
        List<ScoredSemanticMemory> matches = semanticStore.recall(
            new SemanticMemoryQuery(learnerId, types, topK), queryEmbedding.vector(), topK);
        return SemanticRecallResult.available(matches);
    }

    public MemoryPersistenceResult promote(String learnerId, MemoryCandidate candidate) {
        if (candidate == null) {
            return MemoryPersistenceResult.noCandidate();
        }
        PromotionDecision decision = promotionPolicy.evaluate(learnerId, candidate);
        if (!decision.accepted()) {
            return MemoryPersistenceResult.rejected(decision.reason());
        }
        try {
            EmbeddingResult embedded = embeddings.embed(candidate.content());
            if (embedded == null || !embedded.available()) {
                return MemoryPersistenceResult.failed();
            }
            List<AgentSemanticMemory> existing = semanticStore.findByLearnerAndTypes(learnerId, Set.of(candidate.memoryType()));
            AgentSemanticMemory duplicate = existing.stream()
                .filter(memory -> memory.contentFingerprint().equals(candidate.contentFingerprint()))
                .findFirst().orElse(null);
            AgentSemanticMemory conflict = existing.stream()
                .filter(memory -> memory.sourceRef().equals(candidate.sourceRef()))
                .findFirst().orElse(null);
            Instant timestamp = now.get();
            if (duplicate != null && duplicate.confidence() > candidate.confidence()) {
                return MemoryPersistenceResult.persisted(duplicate.id());
            }
            if (conflict != null && duplicate == null && conflict.confidence() >= candidate.confidence()) {
                return MemoryPersistenceResult.persisted(conflict.id());
            }
            AgentSemanticMemory baseline = duplicate != null ? duplicate : conflict;
            String id = baseline == null ? stableId(candidate) : baseline.id();
            Instant createdAt = baseline == null ? timestamp : baseline.createdAt();
            String content = duplicate != null ? duplicate.content() : candidate.content().strip();
            AgentSemanticMemory saved = semanticStore.save(new AgentSemanticMemory(
                id, learnerId, candidate.memoryType(), content, candidate.contentFingerprint(), embedded.vector(),
                candidate.confidence(), candidate.sourceRunId(), candidate.sourceRef(), createdAt, timestamp));
            return MemoryPersistenceResult.persisted(saved.id());
        } catch (RuntimeException persistenceFailure) {
            return MemoryPersistenceResult.failed();
        }
    }

    public MemoryDeletionResult deleteLearnerMemory(String learnerId) {
        requireText(learnerId, "learnerId");
        LayerDeletion sessionDeletion;
        try {
            sessionDeletion = sessions == null ? LayerDeletion.succeeded(0) : LayerDeletion.succeeded(sessions.deleteLearner(learnerId));
        } catch (RuntimeException failure) {
            sessionDeletion = LayerDeletion.failed();
        }
        LayerDeletion semanticDeletion;
        try {
            semanticDeletion = LayerDeletion.succeeded(semanticStore.deleteByLearner(learnerId));
        } catch (RuntimeException failure) {
            semanticDeletion = LayerDeletion.failed();
        }
        return new MemoryDeletionResult(sessionDeletion, semanticDeletion);
    }

    private static String stableId(MemoryCandidate candidate) {
        String stable = candidate.learnerId() + ":" + candidate.memoryType() + ":" + candidate.contentFingerprint();
        return UUID.nameUUIDFromBytes(stable.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}

package com.suilearn.api.rag.index;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

public final class IndexVersionManager {
    private final IndexVersionRepository repository;
    private final Clock clock;

    public IndexVersionManager(IndexVersionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public IndexStatus status(String knowledgeBaseId, EmbeddingSignature signature) {
        var ready = repository.findFirstByKnowledgeBaseIdAndSignatureAndReadyTrueOrderByVersionNoDesc(
            knowledgeBaseId, signature.hash());
        if (ready.isPresent()) {
            return IndexStatus.current(ready.orElseThrow());
        }
        var latest = repository.findByKnowledgeBaseIdOrderByVersionNoDesc(knowledgeBaseId).stream().findFirst();
        return IndexStatus.needsReindex(latest.orElse(null));
    }

    public IndexVersionEntity beginVersion(String knowledgeBaseId, EmbeddingSignature signature, String storageRef) {
        var existing = repository.findByKnowledgeBaseIdOrderByVersionNoDesc(knowledgeBaseId);
        long next = existing.isEmpty() ? 1 : existing.get(0).getVersionNo() + 1;
        boolean ready = existing.isEmpty();
        return repository.save(new IndexVersionEntity(newId(), knowledgeBaseId, signature.hash(), next,
            storageRef, ready, clock.instant()));
    }

    public void markReady(String id) {
        repository.findById(id).ifPresent(entity ->
            repository.save(new IndexVersionEntity(entity.getId(), entity.getKnowledgeBaseId(), entity.getSignature(),
                entity.getVersionNo(), entity.getStorageRef(), true, entity.getCreatedAt())));
    }

    public record IndexStatus(boolean needsReindex, IndexVersionEntity current, IndexVersionEntity latest) {
        public static IndexStatus current(IndexVersionEntity current) {
            return new IndexStatus(false, current, current);
        }
        public static IndexStatus needsReindex(IndexVersionEntity latest) {
            return new IndexStatus(true, null, latest);
        }
    }

    private String newId() { return "idx_" + UUID.randomUUID().toString().replace("-", ""); }
}

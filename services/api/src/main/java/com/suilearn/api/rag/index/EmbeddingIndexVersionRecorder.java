package com.suilearn.api.rag.index;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.retrieval.EmbeddingProvider;
import java.util.Optional;

/**
 * Records a ready index version after a material embedding batch succeeds.
 * Same-signature batches are idempotent; a changed signature starts a new
 * version and marks it ready only after the embedding replacement succeeded.
 */
public final class EmbeddingIndexVersionRecorder {
    private final IndexVersionManager manager;

    public EmbeddingIndexVersionRecorder(IndexVersionManager manager) {
        this.manager = manager;
    }

    public Optional<IndexVersionEntity> recordReadyVersion(String knowledgeBaseId, MaterialChunk sample,
                                                           EmbeddingProvider provider, String storageRef) {
        if (knowledgeBaseId == null || knowledgeBaseId.isBlank() || sample == null
            || sample.embeddingDimensions() == null || sample.embeddingDimensions() < 1) {
            return Optional.empty();
        }
        var signature = new EmbeddingSignature(provider.binding(), provider.model(),
            sample.embeddingDimensions(), provider.baseUrl(), provider.apiVersion());
        var status = manager.status(knowledgeBaseId, signature);
        if (!status.needsReindex()) {
            return Optional.ofNullable(status.current());
        }
        var version = manager.beginVersion(knowledgeBaseId, signature, storageRef);
        manager.markReady(version.getId());
        return Optional.of(version);
    }
}

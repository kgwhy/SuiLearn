package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.rag.index.EmbeddingIndexVersionRecorder;
import com.suilearn.api.rag.index.EmbeddingSignature;
import com.suilearn.api.rag.index.IndexVersionEntity;
import com.suilearn.api.rag.index.IndexVersionManager;
import com.suilearn.api.retrieval.EmbeddingProvider;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmbeddingIndexVersionRecorderTest {
    @Test
    void recordsAndMarksReadyNewSignature() {
        var manager = mock(IndexVersionManager.class);
        var current = new IndexVersionEntity("new", "kb", "newhash", 2, "pgvector-hybrid:mat", true, Instant.EPOCH);
        var latest = new IndexVersionEntity("old", "kb", "oldhash", 1, "pgvector-hybrid:old", true, Instant.EPOCH);
        when(manager.status(eq("kb"), any(EmbeddingSignature.class)))
            .thenReturn(IndexVersionManager.IndexStatus.needsReindex(latest));
        when(manager.beginVersion(eq("kb"), any(EmbeddingSignature.class), anyString())).thenReturn(current);
        var recorder = new EmbeddingIndexVersionRecorder(manager);

        var result = recorder.recordReadyVersion("kb", chunk(1536), provider(), "pgvector-hybrid:mat");

        assertThat(result).contains(current);
        verify(manager).beginVersion(eq("kb"), any(EmbeddingSignature.class), eq("pgvector-hybrid:mat"));
        verify(manager).markReady("new");
    }

    @Test
    void currentSignatureIsIdempotent() {
        var manager = mock(IndexVersionManager.class);
        var current = new IndexVersionEntity("current", "kb", "hash", 3, "ref", true, Instant.EPOCH);
        when(manager.status(eq("kb"), any(EmbeddingSignature.class)))
            .thenReturn(IndexVersionManager.IndexStatus.current(current));
        var recorder = new EmbeddingIndexVersionRecorder(manager);

        var result = recorder.recordReadyVersion("kb", chunk(1536), provider(), "ref2");

        assertThat(result).contains(current);
        verify(manager, never()).beginVersion(anyString(), any(EmbeddingSignature.class), anyString());
        verify(manager, never()).markReady(anyString());
    }

    @Test
    void textOnlySampleWithoutDimensionsIsIgnored() {
        var manager = mock(IndexVersionManager.class);
        var recorder = new EmbeddingIndexVersionRecorder(manager);

        var result = recorder.recordReadyVersion("kb", textOnlyChunk(), provider(), "ref");

        assertThat(result).isEmpty();
        verify(manager, never()).status(anyString(), any(EmbeddingSignature.class));
    }

    private MaterialChunk chunk(int dimensions) {
        return new MaterialChunk("c1", "kb", "mat", "text", 0, null,
            java.util.List.of(1.0d), EmbeddingStatus.READY, "text-embedding-3-small", dimensions);
    }

    private MaterialChunk textOnlyChunk() {
        return new MaterialChunk("c1", "kb", "mat", "text", 0, null,
            null, EmbeddingStatus.TEXT_ONLY, null, null);
    }

    private EmbeddingProvider provider() {
        return new EmbeddingProvider() {
            @Override public Embedding embed(String input) { return new Embedding(java.util.List.of(1.0d)); }
            @Override public String binding() { return "openai-compatible"; }
            @Override public String model() { return "text-embedding-3-small"; }
            @Override public String baseUrl() { return "https://example.test/v1"; }
        };
    }
}

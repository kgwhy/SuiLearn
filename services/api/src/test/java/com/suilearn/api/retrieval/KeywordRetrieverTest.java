package com.suilearn.api.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KeywordRetrieverTest {
    @Test
    void retrievesTextOnlyChunksWithoutCallingEmbeddingProvider() {
        var store = mock(SuiLearnV2Store.class);
        var material = material();
        var chunk = textOnlyChunk();
        when(store.listKnowledgePoints()).thenReturn(List.of());
        when(store.listChunksByScope("kb_1", null)).thenReturn(List.of(chunk));
        when(store.listQuestions()).thenReturn(List.of());
        when(store.listGeneratedContents()).thenReturn(List.of());
        when(store.findMaterial("mat_1")).thenReturn(Optional.of(material));
        var retriever = new KeywordRetriever(new TextOnlyEmbeddingProvider(), store, new TextSearchTokenizer());

        var results = retriever.search(new Retriever.RetrievalRequest("HashMap collision", "kb_1", null));
        var evidence = retriever.retrieveEvidence(new Retriever.RetrievalRequest("HashMap collision", "kb_1", null), 3);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.type()).isEqualTo(SearchResultType.MATERIAL_CHUNK);
            assertThat(result.id()).isEqualTo("chunk_1");
            assertThat(result.score()).isGreaterThan(0.0);
        });
        assertThat(evidence).containsExactly(chunk);
    }

    @Test
    void scoresChineseChunkViaNgramTokenizationInFallbackPath() {
        var store = mock(SuiLearnV2Store.class);
        var material = material();
        var chunk = textOnlyChunk("chunk_1", "机器学习是人工智能的一个分支。", 0);
        when(store.listKnowledgePoints()).thenReturn(List.of());
        when(store.listChunksByScope("kb_1", null)).thenReturn(List.of(chunk));
        when(store.listQuestions()).thenReturn(List.of());
        when(store.listGeneratedContents()).thenReturn(List.of());
        when(store.findMaterial("mat_1")).thenReturn(Optional.of(material));
        var retriever = new KeywordRetriever(new TextOnlyEmbeddingProvider(), store, new TextSearchTokenizer());

        var results = retriever.search(new Retriever.RetrievalRequest("机器学习", "kb_1", null));
        var evidence = retriever.retrieveEvidence(new Retriever.RetrievalRequest("机器学习", "kb_1", null), 3);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.id()).isEqualTo("chunk_1");
            assertThat(result.score()).isGreaterThan(0.0);
        });
        assertThat(evidence).containsExactly(chunk);
    }

    @Test
    void expandsAdjacentChunksAroundBestTextOnlyHit() {
        var store = mock(SuiLearnV2Store.class);
        var material = material();
        var intro = textOnlyChunk("chunk_0", "HashMap stores entries in buckets.", 0);
        var hit = textOnlyChunk("chunk_1", "HashMap collision handling uses linked lists.", 1);
        var next = textOnlyChunk("chunk_2", "Tree bins are used after collision chains grow.", 2);
        when(store.listChunksByScope("kb_1", null)).thenReturn(List.of(intro, hit, next));
        when(store.findMaterial("mat_1")).thenReturn(Optional.of(material));
        var retriever = new KeywordRetriever(new TextOnlyEmbeddingProvider(), store, new TextSearchTokenizer());

        var evidence = retriever.retrieveEvidence(new Retriever.RetrievalRequest("collision handling", "kb_1", null), 3);

        assertThat(evidence).extracting(MaterialChunk::id)
            .containsExactly("chunk_1", "chunk_0", "chunk_2");
    }

    @Test
    void fallsBackToTextOnlyRetrievalWhenEmbeddingProviderFails() {
        var store = mock(SuiLearnV2Store.class);
        var material = material();
        var chunk = textOnlyChunk();
        when(store.listKnowledgePoints()).thenReturn(List.of());
        when(store.listChunksByScope("kb_1", null)).thenReturn(List.of(chunk));
        when(store.listQuestions()).thenReturn(List.of());
        when(store.listGeneratedContents()).thenReturn(List.of());
        when(store.findMaterial("mat_1")).thenReturn(Optional.of(material));
        var retriever = new KeywordRetriever(new FailingEmbeddingProvider(), store, new TextSearchTokenizer());

        var results = retriever.search(new Retriever.RetrievalRequest("HashMap collision", "kb_1", null));
        var evidence = retriever.retrieveEvidence(new Retriever.RetrievalRequest("HashMap collision", "kb_1", null), 3);

        assertThat(results).singleElement()
            .satisfies(result -> assertThat(result.id()).isEqualTo("chunk_1"));
        assertThat(evidence).containsExactly(chunk);
    }

    private LearningMaterial material() {
        return new LearningMaterial(
            "mat_1",
            "kb_1",
            "HashMap Notes",
            MaterialSourceType.MARKDOWN,
            MaterialStatus.READY,
            "HashMap uses buckets and handles collision with linked lists.",
            Instant.parse("2026-05-25T00:00:00Z"),
            null
        );
    }

    private MaterialChunk textOnlyChunk() {
        return textOnlyChunk("chunk_1", "HashMap collision handling uses linked lists.", 0);
    }

    private MaterialChunk textOnlyChunk(String id, String content, int ordinal) {
        return new MaterialChunk(
            id,
            "kb_1",
            "mat_1",
            content,
            ordinal,
            new SourceRef(
                SourceType.MATERIAL_CHUNK,
                id,
                "kb_1",
                "HashMap Notes",
                "mat_1",
                id,
                false,
                content
            ),
            null,
            EmbeddingStatus.TEXT_ONLY,
            null,
            null
        );
    }

    private static class TextOnlyEmbeddingProvider implements EmbeddingProvider {
        @Override
        public Embedding embed(String input) {
            throw new IllegalStateException("embedding should not be called");
        }

        @Override
        public boolean supportsEmbeddings() {
            return false;
        }
    }

    private static class FailingEmbeddingProvider implements EmbeddingProvider {
        @Override
        public Embedding embed(String input) {
            throw new IllegalStateException("OpenAI-compatible embeddings returned HTTP 404");
        }

        @Override
        public boolean supportsEmbeddings() {
            return true;
        }
    }
}

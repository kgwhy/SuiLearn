package com.suilearn.api.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.rag.pipeline.RagPipeline;
import com.suilearn.api.retrieval.Retriever;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchServiceTest {
    @Test
    void searchGoesThroughRagPipeline() {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var pipeline = mock(RagPipeline.class);
        when(knowledgeBases.find("kb-1")).thenReturn(Optional.of(
            new KnowledgeBase("kb-1", "Java", "", Instant.EPOCH, Instant.EPOCH)));
        when(pipeline.search(new Retriever.RetrievalRequest("java", "kb-1", null, 10))).thenReturn(List.of(
            new SearchResult("r1", SearchResultType.MATERIAL_CHUNK, "title", "summary", 0.9d,
                "kb-1", List.of(), List.of())));
        var service = new SearchService(knowledgeBases, materials, pipeline);

        var results = service.search("java", "kb-1", null);

        assertThat(results).singleElement().satisfies(result -> assertThat(result.id()).isEqualTo("r1"));
        var request = ArgumentCaptor.forClass(Retriever.RetrievalRequest.class);
        verify(pipeline).search(request.capture());
        assertThat(request.getValue().query()).isEqualTo("java");
    }
}

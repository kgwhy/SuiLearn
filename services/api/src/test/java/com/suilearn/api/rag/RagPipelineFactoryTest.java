package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.rag.pipeline.PipelineFactory;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPipelineFactoryTest {
    @Test
    void defaultsToPgvectorHybridAndRejectsUnknown() {
        Retriever retriever = new Retriever() {
            @Override public List<SearchResult> search(RetrievalRequest request) { return List.of(); }
            @Override public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) { return List.of(); }
        };
        var factory = PipelineFactory.defaults(retriever);
        assertThat(factory.pipeline(null).name()).isEqualTo("pgvector-hybrid");
        assertThatThrownBy(() -> factory.pipeline("missing")).isInstanceOf(IllegalArgumentException.class);
    }
}

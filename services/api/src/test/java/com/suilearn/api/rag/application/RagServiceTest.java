package com.suilearn.api.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.rag.pipeline.RagPipeline;
import com.suilearn.api.retrieval.Retriever;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagServiceTest {
    @Test
    void askGoesThroughRagPipeline() {
        var ai = mock(AiProvider.class);
        var validator = mock(CitationValidator.class);
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var pipeline = mock(RagPipeline.class);
        var chunk = new MaterialChunk("c1", "kb-1", "mat-1", "evidence", 0,
            new SourceRef(SourceType.MATERIAL, "mat-1", "kb-1", "title", "mat-1", "c1", false, "excerpt"),
            null, EmbeddingStatus.TEXT_ONLY, null, null);
        when(knowledgeBases.find("kb-1")).thenReturn(Optional.of(
            new KnowledgeBase("kb-1", "Java", "", Instant.EPOCH, Instant.EPOCH)));
        when(pipeline.retrieveEvidence(new Retriever.RetrievalRequest("question", "kb-1", null), 5))
            .thenReturn(List.of(chunk));
        when(ai.answerQuestion(org.mockito.ArgumentMatchers.any())).thenReturn(
            new AiProvider.GeneratedAnswer("answer [1]", false, List.of()));
        when(validator.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
            .thenReturn(new CitationValidator.ValidationResult(true, null));
        var service = new RagService(ai, validator, knowledgeBases, materials, pipeline);

        var answer = service.ask("question", "kb-1", null);

        assertThat(answer.answer()).isEqualTo("answer [1]");
        var request = ArgumentCaptor.forClass(Retriever.RetrievalRequest.class);
        verify(pipeline).retrieveEvidence(request.capture(), org.mockito.ArgumentMatchers.eq(5));
        assertThat(request.getValue().query()).isEqualTo("question");
    }
}

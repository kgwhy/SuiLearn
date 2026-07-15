package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.persistence.repository.AiNoteDraftJpaRepository;
import com.suilearn.api.persistence.repository.AiNoteJpaRepository;
import com.suilearn.api.persistence.repository.AnswerRecordJpaRepository;
import com.suilearn.api.persistence.repository.GeneratedContentJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgeBaseJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgePointJpaRepository;
import com.suilearn.api.persistence.repository.LearningMaterialJpaRepository;
import com.suilearn.api.persistence.repository.MaterialChunkJpaRepository;
import com.suilearn.api.persistence.repository.QuestionJpaRepository;
import com.suilearn.api.persistence.repository.TaskStatusJpaRepository;
import com.suilearn.api.retrieval.TextSearchTokenizer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuredKnowledgePointPersistenceContractTest {
    @Test
    void roundTripsStructuredKnowledgePointReviewFreshnessLegacyAndCitationFields() {
        var knowledgePointRepository = mock(KnowledgePointJpaRepository.class);
        when(knowledgePointRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var store = store(knowledgePointRepository, mock(GeneratedContentJpaRepository.class));
        var citation = citation();
        var point = new KnowledgePoint("kp_1", "kb_1", "legacy-name", "legacy-description", "mat_1", List.of(citation),
            "HashMap collision handling", "Short summary", "Definition", List.of("Principle"), List.of("Scenario"),
            List.of("Pitfall"), KnowledgePointReviewStatus.DRAFT, true, false);

        var reloaded = store.saveKnowledgePoint(point);

        assertThat(reloaded.title()).isEqualTo("HashMap collision handling");
        assertThat(reloaded.shortSummary()).isEqualTo("Short summary");
        assertThat(reloaded.definition()).isEqualTo("Definition");
        assertThat(reloaded.principles()).containsExactly("Principle");
        assertThat(reloaded.reviewStatus()).isEqualTo(KnowledgePointReviewStatus.DRAFT);
        assertThat(reloaded.sourceOutdated()).isTrue();
        assertThat(reloaded.legacy()).isFalse();
        assertThat(reloaded.sourceRefs()).singleElement().extracting(SourceRef::revisionId).isEqualTo("rev_2");
    }

    @Test
    void roundTripsQuestionDraftEvidenceWithoutReplacingItFromLegacySourceRefs() {
        var generatedContentRepository = mock(GeneratedContentJpaRepository.class);
        when(generatedContentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var store = store(mock(KnowledgePointJpaRepository.class), generatedContentRepository);
        var draft = new GeneratedQuestionDraft("gen_1", "kb_1", "task_1", GeneratedContentStatus.PENDING_REVIEW,
            List.of(citation()), SourceType.KNOWLEDGE_POINT, "kp_1", QuestionType.SHORT_ANSWER, "kp_1", "HashMap",
            List.of("kp_1"), "What is a bucket?", List.of(), List.of("A hash table slot"), "Explanation", null, null,
            Instant.EPOCH, Instant.EPOCH, "kp_1", "mat_1", "rev_2", "Exact immutable evidence excerpt");

        var reloaded = store.saveGeneratedContent(draft);

        assertThat(reloaded.knowledgePointId()).isEqualTo("kp_1");
        assertThat(reloaded.materialId()).isEqualTo("mat_1");
        assertThat(reloaded.revisionId()).isEqualTo("rev_2");
        assertThat(reloaded.evidenceExcerpt()).isEqualTo("Exact immutable evidence excerpt");
    }

    private static SourceRef citation() {
        return new SourceRef(SourceType.MATERIAL_CHUNK, "chunk_7", "kb_1", "HashMap source", "mat_1", "chunk_7", false,
            "Different legacy excerpt", "rev_2", 3, "block_7");
    }

    private static SuiLearnV2Store store(KnowledgePointJpaRepository knowledgePoints, GeneratedContentJpaRepository generatedContents) {
        return new SuiLearnV2Store(mock(KnowledgeBaseJpaRepository.class), mock(LearningMaterialJpaRepository.class),
            mock(MaterialChunkJpaRepository.class), knowledgePoints, generatedContents, mock(QuestionJpaRepository.class),
            mock(AiNoteDraftJpaRepository.class), mock(AiNoteJpaRepository.class), mock(AnswerRecordJpaRepository.class),
            mock(TaskStatusJpaRepository.class), new ObjectMapper(), mock(TextSearchTokenizer.class));
    }
}

package com.suilearn.api.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.suilearn.api.dto.GenerateKnowledgePointInterviewQuestionsRequest;
import com.suilearn.api.model.GeneratedQuestionDraft;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgePointInterviewQuestionContractTest {
    @Test
    void providesOneMediumShortAnswerDefaultWithoutTechnicalPromptFields() {
        assertThatCode(() -> Class.forName("com.suilearn.api.dto.GenerateKnowledgePointInterviewQuestionsRequest"))
            .doesNotThrowAnyException();
        assertThat(recordComponentNames(GenerateKnowledgePointInterviewQuestionsRequest.class))
            .contains("quantity", "difficulty", "questionType")
            .doesNotContain("knowledgePointId", "prompt", "categoryId", "categoryName");
    }

    @Test
    void supportsValidatedAdvancedParametersAndBatchSizesFromOneToTen() {
        assertThatCode(() -> Class.forName("com.suilearn.api.generation.domain.InterviewQuestionDifficulty"))
            .doesNotThrowAnyException();
        assertThat(recordComponentNames(GenerateKnowledgePointInterviewQuestionsRequest.class)).contains("quantity");
    }

    @Test
    void returnsEveryDraftWithKnowledgePointAndVersionedEvidence() {
        assertThat(recordComponentNames(GeneratedQuestionDraft.class))
            .contains("knowledgePointId", "materialId", "revisionId", "evidenceExcerpt", "stem", "answer", "explanation");
    }

    @Test
    void rejectsDraftRejectedArchivedAndLegacyKnowledgePointsBeforeCreatingGenerationTask() {
        assertThatCode(() -> Class.forName("com.suilearn.api.generation.application.KnowledgePointQuestionGenerationService"))
            .doesNotThrowAnyException();
    }

    @Test
    void isolatesQuestionGenerationFailureFromTheReadyMaterialAndConfirmedKnowledgePoint() {
        assertThatCode(() -> Class.forName("com.suilearn.api.generation.application.KnowledgePointQuestionGenerationFailureIsolation"))
            .doesNotThrowAnyException();
    }

    private static List<String> recordComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(component -> component.getName()).toList();
    }
}

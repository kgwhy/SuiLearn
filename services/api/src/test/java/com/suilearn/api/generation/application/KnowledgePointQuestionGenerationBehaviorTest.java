package com.suilearn.api.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.controller.KnowledgeBaseController;
import com.suilearn.api.controller.TaskController;
import com.suilearn.api.dto.TaskSubmissionResponse;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateKnowledgePointInterviewQuestionsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class KnowledgePointQuestionGenerationBehaviorTest {
    @Test
    void sendsQuestionDifficultyToTheAiPromptInsteadOfDroppingItAtTheApplicationBoundary() {
        assertThat(Arrays.stream(AiProvider.QuestionGenerationPrompt.class.getRecordComponents())
            .map(RecordComponent::getName).toList()).contains("difficulty");
    }

    @Test
    void keepsLegacyGenericGenerateQuestionJsonShapeReadableForOneCompatibilityCycle() {
        var request = new GenerateQuestionRequest("kb_1", List.of(citation()), SourceType.MATERIAL_CHUNK, "chunk_7",
            com.suilearn.api.model.QuestionType.SHORT_ANSWER, "legacy-category", "Legacy category", List.of("kp_1"), "legacy prompt");

        assertThat(request.knowledgePointIds()).containsExactly("kp_1");
        assertThat(request.sourceRefs()).containsExactly(citation());
        assertThat(request.prompt()).isEqualTo("legacy prompt");
    }

    @Test
    void deserializesDeprecatedGenerateQuestionRequestJsonWithoutDroppingSourceRefs() throws Exception {
        var legacyJson = """
            {"knowledgeBaseId":"kb_1","sourceRefs":[{"type":"MATERIAL_CHUNK","id":"chunk_7","knowledgeBaseId":"kb_1","title":"HashMap","materialId":"mat_1","chunkId":"chunk_7","deleted":false,"excerpt":"evidence","revisionId":"rev_2","pageNumber":3,"blockId":"block_7"}],"sourceType":"MATERIAL_CHUNK","sourceId":"chunk_7","questionType":"SHORT_ANSWER","categoryId":"legacy","categoryName":"Legacy","knowledgePointIds":["kp_1"],"prompt":"legacy prompt"}
            """;

        assertThatCode(() -> new ObjectMapper().readValue(legacyJson, GenerateQuestionRequest.class))
            .doesNotThrowAnyException();
    }

    @Test
    void acceptsOmittedInterviewQuestionBodyAndAppliesOneMediumShortAnswerDefaults() {
        assertThatCode(() -> Class.forName("com.suilearn.api.dto.GenerateKnowledgePointsRequest"))
            .doesNotThrowAnyException();
    }

    @Test
    void usesAClosedKnowledgePointQuestionReviewDtoThatCannotOverrideAttributionOrEvidence() {
        assertThatCode(() -> Class.forName("com.suilearn.api.dto.ReviewKnowledgePointQuestionDraftRequest"))
            .doesNotThrowAnyException();
    }

    @Test
    void acceptsAdvancedInterviewQuestionBodyWithoutKnowledgePointIdBecauseThePathSuppliesIt() {
        var request = new GenerateKnowledgePointInterviewQuestionsRequest(null, 2,
            com.suilearn.api.generation.domain.InterviewQuestionDifficulty.HARD,
            com.suilearn.api.model.QuestionType.SHORT_ANSWER);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(request)).isEmpty();
        }
    }

    @Test
    void keepsReprocessedOldRevisionEvidenceAddressableForKnowledgePointGeneration() {
        assertThatCode(() -> Class.forName("com.suilearn.api.material.application.RevisionEvidenceResolver"))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsRevisionEvidenceRequestedForAnotherMaterialInsteadOfReturningOrRelabelingBlocks() {
        assertThatCode(() -> Class.forName("com.suilearn.api.material.application.RevisionEvidenceResolver")
            .getMethod("resolve", String.class, String.class))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsGenericGeneratedContentReviewForKnowledgePointQuestionDrafts() {
        assertThatCode(() -> Class.forName("com.suilearn.api.generation.application.KnowledgePointQuestionDraftReviewService"))
            .doesNotThrowAnyException();
    }

    @Test
    void exposesBothKnowledgePointGenerationRoutesAsAsyncTaskSubmissions() {
        assertThat(postMappings(KnowledgeBaseController.class))
            .contains("/materials/{materialId}/knowledge-point-generations", "/knowledge-points/{knowledgePointId}/interview-question-generations");
        assertThat(postMappings(com.suilearn.api.controller.AiGenerationController.class))
            .doesNotContain("/knowledge-points/{knowledgePointId}/interview-question-generations");
        assertThat(genericReturnType(KnowledgeBaseController.class, "/materials/{materialId}/knowledge-point-generations"))
            .contains(TaskSubmissionResponse.class.getSimpleName());
        assertThat(genericReturnType(KnowledgeBaseController.class, "/knowledge-points/{knowledgePointId}/interview-question-generations"))
            .contains(TaskSubmissionResponse.class.getSimpleName());
    }

    @Test
    void exposesQuestionDraftsForTheCompletedGenerationTask() {
        assertThat(getMappings(TaskController.class)).contains("/{taskId}/question-drafts");
        assertThat(genericReturnType(TaskController.class, "/{taskId}/question-drafts"))
            .contains(GeneratedQuestionDraft.class.getSimpleName());
    }

    @Test
    void exposesExplicitKnowledgePointConfirmationAndRejectionTransitions() {
        assertThat(postMappings(KnowledgeBaseController.class))
            .contains("/knowledge-points/{knowledgePointId}/confirm", "/knowledge-points/{knowledgePointId}/reject");
    }

    @Test
    void reviewBoundaryCannotAcceptKnowledgePointOrCitationReplacementFields() {
        assertThatCode(() -> Class.forName("com.suilearn.api.dto.ReviewKnowledgePointQuestionDraftRequest"))
            .doesNotThrowAnyException();
    }

    @Test
    void sourceOutdatedAndNonConfirmedKnowledgePointsAreRepresentableAsQuestionGenerationPreconditions() {
        var point = new KnowledgePoint("kp_1", "kb_1", "HashMap", "summary", "mat_1", List.of(citation()), "HashMap", "summary",
            "definition", List.of("principle"), List.of("scenario"), List.of("pitfall"), KnowledgePointReviewStatus.DRAFT, true, false);

        assertThat(point.reviewStatus()).isNotEqualTo(KnowledgePointReviewStatus.CONFIRMED);
        assertThat(point.sourceOutdated()).isTrue();
    }

    private static List<String> postMappings(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .flatMap(method -> Arrays.stream(method.getAnnotationsByType(PostMapping.class)))
            .flatMap(mapping -> Arrays.stream(mapping.value())).toList();
    }

    private static List<String> getMappings(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .flatMap(method -> Arrays.stream(method.getAnnotationsByType(GetMapping.class)))
            .flatMap(mapping -> Arrays.stream(mapping.value())).toList();
    }

    private static String genericReturnType(Class<?> type, String mappingPath) {
        return Arrays.stream(type.getDeclaredMethods())
            .filter(method -> Arrays.stream(method.getAnnotationsByType(PostMapping.class))
                .flatMap(mapping -> Arrays.stream(mapping.value())).anyMatch(mappingPath::equals)
                || Arrays.stream(method.getAnnotationsByType(GetMapping.class))
                .flatMap(mapping -> Arrays.stream(mapping.value())).anyMatch(mappingPath::equals))
            .findFirst().map(Method::getGenericReturnType).map(Object::toString).orElseThrow();
    }

    private static SourceRef citation() {
        return new SourceRef(SourceType.MATERIAL_CHUNK, "chunk_7", "kb_1", "HashMap source", "mat_1", "chunk_7", false,
            "HashMap resolves collisions.", "rev_2", 3, "block_7");
    }
}

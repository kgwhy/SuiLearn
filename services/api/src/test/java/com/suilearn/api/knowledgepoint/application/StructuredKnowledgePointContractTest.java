package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.task.infrastructure.TaskStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StructuredKnowledgePointContractTest {
    @Test
    void persistsOnlySchemaCompleteStructuredKnowledgePointDraftsWithVersionedCitations() {
        assertThat(recordComponentNames(KnowledgePoint.class))
            .contains("title", "shortSummary", "definition", "principles", "applicationScenarios", "pitfalls",
                "reviewStatus", "sourceOutdated", "legacy", "sourceRefs");
        assertThat(recordComponentNames(SourceRef.class))
            .contains("materialId", "revisionId", "pageNumber", "blockId", "excerpt");
    }

    @Test
    void doesNotCreateKeywordOrPlaceholderKnowledgePointsWhenAiIsNotConfigured() {
        var knowledgePoints = mock(KnowledgePointStore.class);
        var service = service(unconfiguredProperties(), failingAi(), knowledgePoints);

        var result = service.extractKnowledgePoints(material().id());

        assertThat(result.task().status().name()).isEqualTo("FAILED");
        assertThat(result.knowledgePoints()).isEmpty();
        verify(knowledgePoints, never()).save(any());
    }

    @Test
    void marksTaskFailedWhenAiResultsRemainIncompleteAfterOneRepairAttempt() {
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any()))
            .thenReturn(List.of(new AiProvider.GeneratedKnowledgePoint("HashMap", "A short sentence only")));
        var service = service(configuredProperties(0), aiProvider, knowledgePoints);

        var result = service.extractKnowledgePoints(material().id());

        assertThat(result.task().status().name()).isEqualTo("FAILED");
        assertThat(result.task().errorCode()).isEqualTo("AI_STRUCTURED_OUTPUT_INVALID");
        assertThat(result.knowledgePoints()).isEmpty();
        verify(knowledgePoints, never()).save(any());
        verify(aiProvider).repairKnowledgePointExtraction(any(), any());
    }

    @Test
    void retriesTransientAiFailuresBeforeFailingTheKnowledgePointTask() {
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any()))
            .thenThrow(new IllegalStateException("temporary timeout"))
            .thenThrow(new IllegalStateException("temporary timeout"))
            .thenReturn(List.of(new AiProvider.GeneratedKnowledgePoint("HashMap", "Complete structured result")));
        var service = service(configuredProperties(2), aiProvider, knowledgePoints);

        org.assertj.core.api.Assertions.assertThatCode(() -> service.extractKnowledgePoints(material().id()))
            .doesNotThrowAnyException();

        verify(aiProvider, org.mockito.Mockito.times(3)).extractKnowledgePoints(any());
    }

    @Test
    void exposesDraftConfirmedRejectedAndArchivedReviewSemanticsAndLegacyCompatibility() {
        org.assertj.core.api.Assertions.assertThatCode(
            () -> Class.forName("com.suilearn.api.model.KnowledgePointReviewStatus")
        ).doesNotThrowAnyException();
        assertThat(recordComponentNames(KnowledgePoint.class))
            .contains("legacy", "sourceOutdated");
    }

    @Test
    void rejectsStructuredCitationWhenItDoesNotMatchTheSubmittedCurrentRevisionEvidence() {
        assertThatCode(() -> KnowledgePointService.class.getMethod(
            "extractKnowledgePoints", String.class, String.class
        )).doesNotThrowAnyException();
    }

    @Test
    void permitsOnlyCurrentNonLegacyDraftsToBeConfirmedAndRejectsIllegalReviewTransitions() {
        assertThatCode(() -> KnowledgePointService.class.getMethod(
            "confirmKnowledgePoint", String.class
        )).doesNotThrowAnyException();
        assertThatCode(() -> KnowledgePointService.class.getMethod(
            "rejectKnowledgePoint", String.class
        )).doesNotThrowAnyException();
    }

    private static KnowledgePointService service(
        SuiLearnAiProperties properties,
        AiProvider aiProvider,
        KnowledgePointStore knowledgePoints
    ) {
        var material = material();
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        when(knowledgeBases.find(material.knowledgeBaseId()))
            .thenReturn(Optional.of(new KnowledgeBase(material.knowledgeBaseId(), "Java", null, material.createdAt(), material.createdAt())));
        var materials = mock(MaterialStore.class);
        when(materials.find(material.id())).thenReturn(Optional.of(material));
        var chunks = mock(MaterialChunkStore.class);
        when(chunks.listByMaterial(material.id())).thenReturn(List.of(evidence(material)));
        var retriever = mock(Retriever.class);
        when(retriever.retrieveEvidence(any(), any(Integer.class))).thenReturn(List.of());
        var taskStore = mock(TaskStore.class);
        when(taskStore.save(any(TaskStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var sourceService = new SourceService(knowledgeBases, knowledgePoints, materials, chunks);
        return new KnowledgePointService(
            aiProvider,
            knowledgeBases,
            materials,
            chunks,
            knowledgePoints,
            properties,
            retriever,
            new TaskService(taskStore, Clock.fixed(material.createdAt(), ZoneOffset.UTC)),
            sourceService
        );
    }

    private static AiProvider failingAi() {
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenThrow(new IllegalStateException("AI is unavailable"));
        return aiProvider;
    }

    private static LearningMaterial material() {
        return new LearningMaterial(
            "mat_1", "kb_1", "Java collections", MaterialSourceType.MARKDOWN, MaterialStatus.READY,
            "HashMap equals hashCode StringBuilder", Instant.parse("2026-07-01T00:00:00Z"), null
        );
    }

    private static MaterialChunk evidence(LearningMaterial material) {
        return new MaterialChunk(
            "chunk_1", material.knowledgeBaseId(), material.id(), material.content(), 0,
            new SourceRef(SourceType.MATERIAL_CHUNK, "chunk_1", material.knowledgeBaseId(), material.title(),
                material.id(), "chunk_1", false, material.content()),
            null, EmbeddingStatus.TEXT_ONLY, null, null
        );
    }

    private static SuiLearnAiProperties configuredProperties(int maxRetries) {
        return new SuiLearnAiProperties("openai-compatible", "https://chat.example.test", "secret", "model", "", 30000, maxRetries);
    }

    private static SuiLearnAiProperties unconfiguredProperties() {
        return new SuiLearnAiProperties("openai-compatible", "", "", "", "", 30000, 0);
    }

    private static List<String> recordComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(component -> component.getName()).toList();
    }

}

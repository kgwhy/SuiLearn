package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KnowledgePointServiceTest {
    @Test
    void failsWithoutPersistingKeywordCandidatesWhenAiProviderIsNotConfigured() {
        var material = new LearningMaterial(
            "mat_1",
            "kb_1",
            "Java 面试资料",
            MaterialSourceType.MARKDOWN,
            MaterialStatus.READY,
            "HashMap equals hashCode StringBuilder",
            Instant.parse("2026-07-01T00:00:00Z"),
            null
        );
        var chunk = new MaterialChunk(
            "chunk_1",
            "kb_1",
            material.id(),
            "HashMap equals hashCode StringBuilder",
            0,
            new SourceRef(
                SourceType.MATERIAL_CHUNK,
                "chunk_1",
                "kb_1",
                material.title(),
                material.id(),
                "chunk_1",
                false,
                "HashMap equals hashCode StringBuilder"
            ),
            null,
            EmbeddingStatus.TEXT_ONLY,
            null,
            null
        );
        var service = serviceWithFailingAi(material, List.of(chunk));

        var result = service.extractKnowledgePoints(material.id());

        assertThat(result.knowledgePoints()).isEmpty();
        assertThat(result.task().status().name()).isEqualTo("FAILED");
        assertThat(result.task().currentStep()).isEqualTo("AI_EXTRACTION_FAILED");
        assertThat(result.task().model()).isNull();
    }

    @Test
    void recordsAiExtractionWhenConfiguredChatProviderReturnsKnowledgePoints() {
        var material = readyMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(new AiProvider.GeneratedKnowledgePoint(
            "HashMap resizing", "AI generated explanation", "HashMap resizing", "AI generated explanation",
            "HashMap resizes buckets when the load factor threshold is exceeded.", List.of("Load factor controls resizing."),
            List.of("Use it to size hash maps."), List.of("Capacity is not the same as size."), List.of(new SourceRef(
                SourceType.MATERIAL_CHUNK, "chunk_1", material.knowledgeBaseId(), material.title(), material.id(), "chunk_1",
                false, "HashMap evidence", "rev_1", 1, "block_1"
            ))
        )));
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(material, List.of(), aiProvider, configuredProperties(), knowledgePoints);

        var result = service.extractKnowledgePoints(material.id());

        assertThat(result.knowledgePoints())
            .extracting(KnowledgePoint::description)
            .containsExactly("AI generated explanation");
        assertThat(result.task().currentStep()).isEqualTo("AI_EXTRACTED");
        assertThat(result.task().model()).isEqualTo("deepseek-v4-flash");
        verify(knowledgePoints).save(any());
    }

    @Test
    void doesNotFallBackWhenConfiguredChatProviderFails() {
        var material = readyMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any()))
            .thenThrow(new IllegalStateException("HTTP 401"));
        var knowledgePoints = mock(KnowledgePointStore.class);
        var service = service(material, List.of(), aiProvider, configuredProperties(), knowledgePoints);

        assertThatThrownBy(() -> service.extractKnowledgePoints(material.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Configured chat AI failed");
        verify(knowledgePoints, never()).save(any());
    }

    @Test
    void repairsOneInvalidStructuredResultBeforePersistingDrafts() {
        var material = readyMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(incompletePoint()));
        when(aiProvider.repairKnowledgePointExtraction(any(), any())).thenReturn(List.of(completePoint(material)));
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(material, List.of(), aiProvider, configuredProperties(), knowledgePoints);

        var result = service.extractKnowledgePoints(material.id());

        assertThat(result.task().status().name()).isEqualTo("SUCCEEDED");
        assertThat(result.knowledgePoints()).singleElement().satisfies(point ->
            assertThat(point.title()).isEqualTo("HashMap resizing"));
        verify(aiProvider, times(1)).repairKnowledgePointExtraction(any(), any());
        verify(knowledgePoints).save(any());
    }

    @Test
    void marksTaskFailedAfterTheOnlyStructuredRepairAlsoFails() {
        var material = readyMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(incompletePoint()));
        when(aiProvider.repairKnowledgePointExtraction(any(), any())).thenReturn(List.of(incompletePoint()));
        var knowledgePoints = mock(KnowledgePointStore.class);
        var service = service(material, List.of(), aiProvider, configuredProperties(), knowledgePoints);

        var result = service.extractKnowledgePoints(material.id());

        assertThat(result.knowledgePoints()).isEmpty();
        assertThat(result.task().status().name()).isEqualTo("FAILED");
        assertThat(result.task().errorCode()).isEqualTo("AI_STRUCTURED_OUTPUT_INVALID");
        assertThat(result.task().errorMessage()).doesNotContain("raw model response");
        verify(aiProvider, times(1)).repairKnowledgePointExtraction(any(), any());
        verify(knowledgePoints, never()).save(any());
    }

    @Test
    void repairsMixedValidAndOutOfScopeCitationsInsteadOfSilentlyDroppingTheOutOfScopeCitation() {
        var material = readyVersionedMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(pointWithMixedCitations(material)));
        when(aiProvider.repairKnowledgePointExtraction(any(), any())).thenReturn(List.of(completePoint(material)));
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(material, List.of(versionedChunk(material)), aiProvider, configuredProperties(), knowledgePoints);

        var result = service.extractKnowledgePoints(material.id());

        assertThat(result.knowledgePoints()).singleElement();
        verify(aiProvider, times(1)).repairKnowledgePointExtraction(any(), any());
    }

    @Test
    void invokesSemanticRepairOnlyOnceWhenConfiguredRetriesAreOne() {
        var material = readyMaterial();
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(incompletePoint()));
        when(aiProvider.repairKnowledgePointExtraction(any(), any())).thenThrow(new IllegalStateException("HTTP 503"));
        var service = service(material, List.of(), aiProvider, propertiesWithRetries(1), mock(KnowledgePointStore.class));

        assertThatThrownBy(() -> service.extractKnowledgePoints(material.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HTTP 503");

        verify(aiProvider, times(1)).repairKnowledgePointExtraction(any(), any());
    }

    private static KnowledgePointService serviceWithFailingAi(LearningMaterial material, List<MaterialChunk> chunks) {
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any()))
            .thenThrow(new IllegalStateException("OpenAI-compatible provider is missing chat baseUrl, chat apiKey, or chatModel"));
        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return service(material, chunks, aiProvider, unconfiguredProperties(), knowledgePoints);
    }

    private static KnowledgePointService service(
        LearningMaterial material,
        List<MaterialChunk> chunks,
        AiProvider aiProvider,
        SuiLearnAiProperties properties,
        KnowledgePointStore knowledgePoints
    ) {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        when(knowledgeBases.find(material.knowledgeBaseId()))
            .thenReturn(Optional.of(new KnowledgeBase(material.knowledgeBaseId(), "Java", null, material.createdAt(), material.createdAt())));

        var materials = mock(MaterialStore.class);
        when(materials.find(material.id())).thenReturn(Optional.of(material));

        var materialChunks = mock(MaterialChunkStore.class);
        when(materialChunks.listByMaterial(material.id())).thenReturn(chunks);

        var retriever = mock(Retriever.class);
        when(retriever.retrieveEvidence(any(), any(Integer.class))).thenReturn(List.of());

        var taskStore = mock(TaskStore.class);
        when(taskStore.save(any(TaskStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var taskService = new TaskService(taskStore, Clock.fixed(material.createdAt(), ZoneOffset.UTC));

        var sourceService = new SourceService(knowledgeBases, knowledgePoints, materials, materialChunks);
        return new KnowledgePointService(
            aiProvider,
            knowledgeBases,
            materials,
            materialChunks,
            knowledgePoints,
            properties,
            retriever,
            taskService,
            sourceService
        );
    }

    private static LearningMaterial readyMaterial() {
        return new LearningMaterial(
            "mat_1",
            "kb_1",
            "Java 面试资料",
            MaterialSourceType.MARKDOWN,
            MaterialStatus.READY,
            "HashMap equals hashCode StringBuilder",
            Instant.parse("2026-07-01T00:00:00Z"),
            null
        );
    }

    private static SuiLearnAiProperties configuredProperties() {
        return new SuiLearnAiProperties(
            "openai-compatible",
            "https://chat.example.test",
            "secret",
            "deepseek-v4-flash",
            "",
            30000,
            0
        );
    }

    private static SuiLearnAiProperties unconfiguredProperties() {
        return new SuiLearnAiProperties(
            "openai-compatible",
            "",
            "",
            "",
            "",
            30000,
            0
        );
    }

    private static SuiLearnAiProperties propertiesWithRetries(int maxRetries) {
        return new SuiLearnAiProperties(
            "openai-compatible", "https://chat.example.test", "secret", "deepseek-v4-flash", "", 30000, maxRetries
        );
    }

    private static AiProvider.GeneratedKnowledgePoint incompletePoint() {
        return new AiProvider.GeneratedKnowledgePoint(
            "HashMap resizing", "", "HashMap resizing", "", "", List.of(), List.of(), List.of(), List.of()
        );
    }

    private static AiProvider.GeneratedKnowledgePoint completePoint(LearningMaterial material) {
        return new AiProvider.GeneratedKnowledgePoint(
            "HashMap resizing", "AI generated explanation", "HashMap resizing", "AI generated explanation",
            "HashMap resizes buckets when the load factor threshold is exceeded.", List.of("Load factor controls resizing."),
            List.of("Use it to size hash maps."), List.of("Capacity is not the same as size."), List.of(new SourceRef(
                SourceType.MATERIAL_CHUNK, "chunk_1", material.knowledgeBaseId(), material.title(), material.id(), "chunk_1",
                false, "HashMap evidence", "rev_1", 1, "block_1"
            ))
        );
    }

    private static LearningMaterial readyVersionedMaterial() {
        return new LearningMaterial(
            "mat_1", "kb_1", "Java 面试资料", MaterialSourceType.MARKDOWN, MaterialStatus.READY,
            null, null, null, "HashMap equals hashCode StringBuilder", Instant.parse("2026-07-01T00:00:00Z"), null, "rev_1"
        );
    }

    private static MaterialChunk versionedChunk(LearningMaterial material) {
        return new MaterialChunk(
            "chunk_1", "kb_1", material.id(), "HashMap evidence", 0, new SourceRef(
                SourceType.MATERIAL_CHUNK, "chunk_1", "kb_1", material.title(), material.id(), "chunk_1", false,
                "HashMap evidence", "rev_1", 1, "block_1"
            ), null, EmbeddingStatus.TEXT_ONLY, null, null
        );
    }

    private static AiProvider.GeneratedKnowledgePoint pointWithMixedCitations(LearningMaterial material) {
        var complete = completePoint(material);
        var outOfScope = new SourceRef(
            SourceType.MATERIAL_CHUNK, "chunk_other", "kb_1", material.title(), material.id(), "chunk_other", false,
            "Other material evidence", "rev_1", 2, "block_other"
        );
        return new AiProvider.GeneratedKnowledgePoint(
            complete.name(), complete.description(), complete.title(), complete.shortSummary(), complete.definition(),
            complete.principles(), complete.applicationScenarios(), complete.pitfalls(), List.of(complete.citations().get(0), outOfScope)
        );
    }
}

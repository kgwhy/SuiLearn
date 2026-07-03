package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.ai.AiProvider;
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
    void fallsBackToLocalExtractionWhenAiProviderIsNotConfigured() {
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

        assertThat(result.knowledgePoints())
            .extracting(KnowledgePoint::name)
            .contains("HashMap", "equals", "hashCode", "StringBuilder");
        assertThat(result.task().status().name()).isEqualTo("SUCCEEDED");
    }

    private static KnowledgePointService serviceWithFailingAi(LearningMaterial material, List<MaterialChunk> chunks) {
        var aiProvider = mock(AiProvider.class);
        when(aiProvider.extractKnowledgePoints(any()))
            .thenThrow(new IllegalStateException("OpenAI-compatible provider is missing chat baseUrl, chat apiKey, or chatModel"));

        var knowledgeBases = mock(KnowledgeBaseStore.class);
        when(knowledgeBases.find(material.knowledgeBaseId()))
            .thenReturn(Optional.of(new KnowledgeBase(material.knowledgeBaseId(), "Java", null, material.createdAt(), material.createdAt())));

        var materials = mock(MaterialStore.class);
        when(materials.find(material.id())).thenReturn(Optional.of(material));

        var materialChunks = mock(MaterialChunkStore.class);
        when(materialChunks.listByMaterial(material.id())).thenReturn(chunks);

        var knowledgePoints = mock(KnowledgePointStore.class);
        when(knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
            retriever,
            taskService,
            sourceService
        );
    }
}

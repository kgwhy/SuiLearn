package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KnowledgePointDurableGenerationServiceTest {
    @Test
    void submitGenerationOnlyCreatesQueuedTaskAndOutboxEventWithoutCallingAi() {
        var fixture = fixture();
        var queued = task("task_1", TaskLifecycleStatus.QUEUED);
        when(fixture.submissions.submit(eq(TaskKind.KNOWLEDGE_POINT_EXTRACTION), eq("kb_1"), eq("mat_1"), any(), any(),
            eq("QUEUED"), eq("GENERATING_KNOWLEDGE_POINTS"), eq("mat_1"), eq("mat_1"))).thenReturn(queued);

        var submitted = fixture.service.submitGeneration("mat_1");

        assertThat(submitted).isSameAs(queued);
        verify(fixture.aiProvider, never()).extractKnowledgePoints(any());
        verify(fixture.submissions).submit(eq(TaskKind.KNOWLEDGE_POINT_EXTRACTION), eq("kb_1"), eq("mat_1"), any(), any(),
            eq("QUEUED"), eq("GENERATING_KNOWLEDGE_POINTS"), eq("mat_1"), eq("mat_1"));
    }

    @Test
    void consumeGenerationExecutesAiAndMarksTheQueuedTaskSucceeded() {
        var fixture = fixture();
        var queued = task("task_1", TaskLifecycleStatus.QUEUED);
        when(fixture.taskService.getTaskStatus(queued.id())).thenReturn(queued);
        when(fixture.taskService.startTask(queued, "EXTRACTING")).thenReturn(task(queued.id(), TaskLifecycleStatus.RUNNING));
        when(fixture.taskService.updateTask(any(), eq(TaskLifecycleStatus.SUCCEEDED), eq(100), eq("AI_EXTRACTED"), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> task(queued.id(), TaskLifecycleStatus.SUCCEEDED));
        when(fixture.aiProvider.extractKnowledgePoints(any())).thenReturn(List.of(point()));
        when(fixture.knowledgePoints.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = fixture.service.consumeGeneration(queued.id(), "mat_1");

        assertThat(result.task().status()).isEqualTo(TaskLifecycleStatus.SUCCEEDED);
        assertThat(result.knowledgePoints()).hasSize(1);
        verify(fixture.aiProvider).extractKnowledgePoints(any());
    }

    @Test
    void consumeGenerationMarksOnlyItsTaskFailedWhenAiExecutionFails() {
        var fixture = fixture();
        var queued = task("task_1", TaskLifecycleStatus.QUEUED);
        when(fixture.taskService.getTaskStatus(queued.id())).thenReturn(queued);
        when(fixture.taskService.startTask(queued, "EXTRACTING")).thenReturn(task(queued.id(), TaskLifecycleStatus.RUNNING));
        when(fixture.aiProvider.extractKnowledgePoints(any())).thenThrow(new IllegalStateException("AI timeout"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fixture.service.consumeGeneration(queued.id(), "mat_1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Configured chat AI failed");

        verify(fixture.taskService).updateTask(any(), eq(TaskLifecycleStatus.FAILED), eq(100), eq("AI_EXTRACTION_FAILED"),
            any(), eq("AI_EXTRACTION_FAILED"), any(), eq("mat_1"), any());
        verify(fixture.knowledgePoints, never()).save(any());
    }

    private static Fixture fixture() {
        var material = new LearningMaterial("mat_1", "kb_1", "Java", MaterialSourceType.MARKDOWN, MaterialStatus.READY,
            "HashMap", Instant.parse("2026-07-01T00:00:00Z"), null);
        var aiProvider = mock(AiProvider.class);
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        when(knowledgeBases.find("kb_1")).thenReturn(Optional.of(new KnowledgeBase("kb_1", "Java", null, material.createdAt(), material.createdAt())));
        var materials = mock(MaterialStore.class);
        when(materials.find("mat_1")).thenReturn(Optional.of(material));
        var chunks = mock(MaterialChunkStore.class);
        when(chunks.listByMaterial("mat_1")).thenReturn(List.of(evidence(material)));
        var knowledgePoints = mock(KnowledgePointStore.class);
        var retriever = mock(Retriever.class);
        when(retriever.retrieveEvidence(any(), any(Integer.class))).thenReturn(List.of());
        var taskService = mock(TaskService.class);
        var submissions = mock(TaskOutboxSubmissionService.class);
        var properties = new SuiLearnAiProperties("openai-compatible", "https://chat.example.test", "secret", "model", "", 30000, 0);
        var service = new KnowledgePointService(aiProvider, knowledgeBases, materials, chunks, knowledgePoints, properties, retriever,
            taskService, new SourceService(knowledgeBases, knowledgePoints, materials, chunks), submissions);
        return new Fixture(service, aiProvider, knowledgePoints, taskService, submissions);
    }

    private static MaterialChunk evidence(LearningMaterial material) {
        return new MaterialChunk("chunk_1", "kb_1", "mat_1", "HashMap", 0,
            new SourceRef(SourceType.MATERIAL_CHUNK, "chunk_1", "kb_1", "Java", "mat_1", "chunk_1", false,
                "HashMap", "rev_1", 1, "block_1"), null, EmbeddingStatus.TEXT_ONLY, null, null);
    }

    private static AiProvider.GeneratedKnowledgePoint point() {
        return new AiProvider.GeneratedKnowledgePoint("HashMap", "summary", "HashMap", "summary", "definition", List.of("principle"),
            List.of("scenario"), List.of("pitfall"), List.of(new SourceRef(SourceType.MATERIAL_CHUNK, "chunk_1", "kb_1", "Java",
                "mat_1", "chunk_1", false, "HashMap", "rev_1", 1, "block_1")));
    }

    private static TaskStatus task(String id, TaskLifecycleStatus status) {
        var now = Instant.parse("2026-07-01T00:00:00Z");
        return new TaskStatus(id, TaskKind.KNOWLEDGE_POINT_EXTRACTION, status, "kb_1", "mat_1", null, null, null, 0,
            status == TaskLifecycleStatus.QUEUED ? "QUEUED" : "EXTRACTING", null, null, 0, null, now, null, null, now);
    }

    private record Fixture(KnowledgePointService service, AiProvider aiProvider, KnowledgePointStore knowledgePoints,
                           TaskService taskService, TaskOutboxSubmissionService submissions) {}
}

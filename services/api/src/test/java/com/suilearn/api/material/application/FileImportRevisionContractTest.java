package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.document.DocumentParser;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class FileImportRevisionContractTest {
    @Test
    void workerHasDurableRevisionAndBlockStoresForFileImports() {
        assertThat(Arrays.stream(MaterialImportService.class.getDeclaredFields())
            .map(Field::getType))
            .contains(DocumentRevisionJpaRepository.class, DocumentBlockJpaRepository.class);
    }

    @Test
    void materialTracksTheCurrentImmutableRevisionInsteadOfOnlyMutableContent() {
        assertThat(LearningMaterial.class.getRecordComponents())
            .extracting(component -> component.getName())
            .contains("currentRevisionId");
    }

    @Test
    void successfulFileWorkerImportPersistsOrderedBlocksAndSetsCurrentRevision() {
        var materials = mock(MaterialStore.class);
        var tasks = mock(TaskService.class);
        var revisions = mock(DocumentRevisionJpaRepository.class);
        var blocks = mock(DocumentBlockJpaRepository.class);
        var originals = mock(OriginalAssetMaterialContentReader.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var task = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, null, null, now);
        var material = new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.DOCX, MaterialStatus.UPLOADED,
            "task_1", null, null, "", now, null);
        when(materials.find("mat_1")).thenReturn(Optional.of(material));
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.getTaskStatus("task_1")).thenReturn(task);
        when(tasks.startTask(task, "UPLOADED")).thenReturn(task);
        when(tasks.updateTask(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(task);
        when(revisions.countByMaterialId("mat_1")).thenReturn(0L);
        when(revisions.findById("rev_task_1")).thenReturn(Optional.of(new DocumentRevisionEntity(
            "rev_task_1", "mat_1", 4, "pending", "FILE_IMPORT", "parser-v1", now
        )));
        when(originals.readDocument(any(), any())).thenReturn(Optional.of(new OriginalAssetMaterialContentReader.ParsedOriginalDocument(
            "First\n\nSecond", List.of(
                new DocumentParser.Block(0, 1, "", "First"),
                new DocumentParser.Block(1, 2, "", "Second")
            )
        )));
        var chunker = mock(MaterialChunker.class);
        when(chunker.chunk(any())).thenReturn(List.of());

        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, mock(MaterialChunkStore.class), mock(MaterialParser.class), chunker,
            noEmbeddings(), Clock.fixed(now, ZoneOffset.UTC), tasks, new TaskExecutor(tasks), new AsyncProcessingAdmissionGuard(true),
            null, null, originals, revisions, blocks
        );

        var result = service.consumeQueuedMaterialImport("mat_1");

        var revision = ArgumentCaptor.forClass(DocumentRevisionEntity.class);
        verify(revisions, org.mockito.Mockito.atLeastOnce()).save(revision.capture());
        var savedBlocks = ArgumentCaptor.forClass(DocumentBlockEntity.class);
        verify(blocks, org.mockito.Mockito.times(2)).save(savedBlocks.capture());
        assertThat(result.currentRevisionId()).isNotBlank();
        assertThat(field(revision.getValue(), "id")).isEqualTo(result.currentRevisionId());
        assertThat(field(revision.getValue(), "revisionNumber")).isEqualTo(4);
        assertThat(savedBlocks.getAllValues()).extracting(block -> field(block, "blockOrder"))
            .containsExactly(0, 1);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static EmbeddingProvider noEmbeddings() {
        return new EmbeddingProvider() {
            @Override public String model() { return "none"; }
            @Override public Embedding embed(String text) { throw new AssertionError("embeddings must not run"); }
            @Override public boolean supportsEmbeddings() { return false; }
        };
    }
}

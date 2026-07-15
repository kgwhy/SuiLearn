package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class MaterialReprocessContractTest {
    @Test
    void admitsReprocessingAsADurableTaskAndReplacesOnlyTheCurrentProcessingTaskReference() {
        var materials = mock(MaterialStore.class);
        var originals = mock(OriginalAssetMaterialContentReader.class);
        var outbox = mock(TaskOutboxSubmissionService.class);
        var existing = material("task_old");
        var task = task("task_reprocess");
        when(materials.find("mat_1")).thenReturn(Optional.of(existing));
        when(originals.hasOriginal(existing)).thenReturn(true);
        when(outbox.submit(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(task);
        when(materials.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(materials, originals, outbox);

        var submitted = service.reprocessMaterial("mat_1");

        assertThat(submitted.id()).isEqualTo("task_reprocess");
        verify(outbox).submit(eq(TaskKind.MATERIAL_REPROCESS), eq("kb_1"), eq("mat_1"), eq(null), eq(null), eq("REPROCESS"),
            eq("REPROCESS"), org.mockito.ArgumentMatchers.startsWith("material-reprocess:mat_1:"), eq("mat_1"));
    }

    @Test
    void givesEachReprocessSubmissionItsOwnOutboxIdempotencyKey() {
        var materials = mock(MaterialStore.class);
        var originals = mock(OriginalAssetMaterialContentReader.class);
        var outbox = mock(TaskOutboxSubmissionService.class);
        var existing = material("task_old");
        when(materials.find("mat_1")).thenReturn(Optional.of(existing));
        when(originals.hasOriginal(existing)).thenReturn(true);
        when(outbox.submit(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(task("task_1"));
        when(materials.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(materials, originals, outbox);

        service.reprocessMaterial("mat_1");
        service.reprocessMaterial("mat_1");

        var keys = ArgumentCaptor.forClass(String.class);
        verify(outbox, org.mockito.Mockito.times(2)).submit(any(), any(), any(), any(), any(), any(), any(), keys.capture(), any());
        assertThat(keys.getAllValues()).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void rejectsReprocessingALegacyTextOnlyMaterialWithTheContractConflict() {
        var materials = mock(MaterialStore.class);
        var originals = mock(OriginalAssetMaterialContentReader.class);
        var existing = material("task_old");
        when(materials.find("mat_1")).thenReturn(Optional.of(existing));
        when(originals.hasOriginal(existing)).thenReturn(false);

        assertThatThrownBy(() -> service(materials, originals, mock(TaskOutboxSubmissionService.class)).reprocessMaterial("mat_1"))
            .isInstanceOf(LegacyMaterialReprocessConflict.class)
            .hasMessage("LEGACY_NO_ORIGINAL");
    }

    private static MaterialImportService service(MaterialStore materials, OriginalAssetMaterialContentReader originals,
                                                 TaskOutboxSubmissionService outbox) {
        return new MaterialImportService(mock(KnowledgeBaseStore.class), materials, mock(MaterialChunkStore.class),
            mock(MaterialParser.class), mock(MaterialChunker.class), noEmbeddings(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            mock(TaskService.class), mock(TaskExecutor.class), new AsyncProcessingAdmissionGuard(true), outbox, null, originals);
    }

    private static LearningMaterial material(String taskId) {
        return new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.PDF, MaterialStatus.READY,
            taskId, null, null, "content", Instant.EPOCH, null, "rev_old");
    }

    private static TaskStatus task(String id) {
        return new TaskStatus(id, TaskKind.MATERIAL_REPROCESS, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "REPROCESS", null, null, 0, null, Instant.EPOCH, null, null, Instant.EPOCH);
    }

    private static EmbeddingProvider noEmbeddings() {
        return new EmbeddingProvider() {
            @Override public String model() { return "none"; }
            @Override public Embedding embed(String text) { throw new AssertionError("embeddings must not run"); }
            @Override public boolean supportsEmbeddings() { return false; }
        };
    }
}

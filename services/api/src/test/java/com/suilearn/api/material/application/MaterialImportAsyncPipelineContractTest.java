package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.controller.KnowledgeBaseController;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.MaterialImportAcceptedResponse;
import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.KnowledgeBase;
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
import java.lang.reflect.Method;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

class MaterialImportAsyncPipelineContractTest {
    @Test
    void multipartImportEndpointReturnsAcceptedResponseWithMaterialAndTaskReferences() {
        Method multipartEndpoint = Arrays.stream(KnowledgeBaseController.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("importMaterialFormData"))
            .filter(method -> Arrays.asList(method.getParameterTypes()).contains(MultipartFile.class))
            .findFirst()
            .orElseThrow();

        assertThat(multipartEndpoint.getReturnType()).isEqualTo(ResponseEntity.class);
        assertThat(multipartEndpoint.getAnnotation(PostMapping.class).consumes())
            .contains("multipart/form-data");
        assertThat(multipartEndpoint.getGenericReturnType().getTypeName())
            .contains(MaterialImportAcceptedResponse.class.getName());
        assertThat(MaterialImportAcceptedResponse.class.getRecordComponents())
            .extracting(component -> component.getName())
            .containsExactly("taskId", "status", "taskHref", "materialId", "materialHref");
    }

    @Test
    void requestThreadPersistsAdmissionButNeverInvokesParser() {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var parser = mock(MaterialParser.class);
        var tasks = mock(TaskService.class);
        var taskExecutor = new TaskExecutor(tasks);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var queued = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, null, null, now);
        var running = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.RUNNING, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, now, null, now);

        when(knowledgeBases.find("kb_1")).thenReturn(Optional.of(new KnowledgeBase("kb_1", "KB", "", now, now)));
        when(tasks.createTask(any(TaskKind.class), any(), any(), any(), any(), any())).thenReturn(queued);
        when(tasks.startTask(queued, "UPLOADED")).thenReturn(running);
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new MaterialImportService(
            knowledgeBases,
            materials,
            mock(MaterialChunkStore.class),
            parser,
            mock(MaterialChunker.class),
            noEmbeddingProvider(),
            Clock.fixed(now, java.time.ZoneOffset.UTC),
            tasks,
            taskExecutor,
            new AsyncProcessingAdmissionGuard(true)
        );

        service.importMaterial("kb_1", new ImportMaterialRequest("Notes", "notes.txt", MaterialSourceType.TXT, "text"));

        verifyNoInteractions(parser);
    }

    @Test
    void asyncAdmissionUsesTransactionalOutboxAndExposesASeparatelyInvocableWorker() {
        assertThat(MaterialImportService.class.getAnnotation(Transactional.class)).isNotNull();
        assertThat(Arrays.stream(MaterialImportService.class.getDeclaredFields())
            .map(field -> field.getType())
            .anyMatch(type -> type == TaskOutboxSubmissionService.class))
            .isTrue();
        assertThat(Arrays.stream(MaterialImportService.class.getDeclaredMethods())
            .map(Method::getName))
            .contains("consumeQueuedMaterialImport");
    }

    @Test
    void multipartAdmissionHasAnOriginalAssetBridgeForTheWorker() {
        Optional<Class<?>> bridge = originalAssetBridge();

        assertThat(bridge).isPresent();
        assertThat(Arrays.stream(MaterialImportService.class.getDeclaredFields())
            .map(field -> field.getType().getName()))
            .contains("com.suilearn.api.material.application.OriginalAssetMaterialContentReader");
    }

    @Test
    void multipartAdmissionStreamsTheSubmittedInputToOriginalAssetStorageWithoutParsing() {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var parser = mock(MaterialParser.class);
        var tasks = mock(TaskService.class);
        var promotions = mock(AssetPromotionCoordinator.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var queued = task("task_1", TaskLifecycleStatus.QUEUED, now);
        var uploadStream = new ByteArrayInputStream("asset text".getBytes());
        var upload = new AssetUpload(uploadStream, "notes.txt", "text/plain");

        when(knowledgeBases.find("kb_1")).thenReturn(Optional.of(new KnowledgeBase("kb_1", "KB", "", now, now)));
        when(tasks.createTask(any(TaskKind.class), any(), any(), any(), any(), any())).thenReturn(queued);
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new MaterialImportService(
            knowledgeBases, materials, mock(MaterialChunkStore.class), parser, mock(MaterialChunker.class),
            noEmbeddingProvider(), Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks),
            new AsyncProcessingAdmissionGuard(true), null, promotions, null
        );

        service.importMultipartMaterial("kb_1", "Notes", "notes.txt", MaterialSourceType.TXT, upload);

        var captured = ArgumentCaptor.forClass(AssetUpload.class);
        verify(promotions).store(captured.capture(), any(), org.mockito.ArgumentMatchers.eq("ORIGINAL"));
        assertThat(captured.getValue().stream()).isSameAs(uploadStream);
        assertThat(captured.getValue().originalFilename()).isEqualTo("notes.txt");
        verifyNoInteractions(parser);
    }

    @Test
    void forgedMultipartInputIsRejectedBeforeTaskOutboxMaterialOrOriginalAssetPersistence() {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var tasks = mock(TaskService.class);
        var promotions = mock(AssetPromotionCoordinator.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        when(knowledgeBases.find("kb_1")).thenReturn(Optional.of(new KnowledgeBase("kb_1", "KB", "", now, now)));
        var service = new MaterialImportService(
            knowledgeBases, materials, mock(MaterialChunkStore.class), mock(MaterialParser.class), mock(MaterialChunker.class),
            noEmbeddingProvider(), Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks),
            new AsyncProcessingAdmissionGuard(true), null, promotions, null, null, null,
            new MaterialUploadValidator(1024, 500)
        );

        assertThatThrownBy(() -> service.importMultipartMaterial("kb_1", "Notes", "notes.pdf", MaterialSourceType.PDF,
            new AssetUpload(new ByteArrayInputStream("forged".getBytes()), "notes.pdf", "application/pdf")))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);

        verifyNoInteractions(materials, tasks, promotions);
    }

    @Test
    void workerReadsOriginalAssetBridgeBeforeFallingBackToLegacyTextParser() {
        var materials = mock(MaterialStore.class);
        var legacyParser = mock(MaterialParser.class);
        var reader = mock(OriginalAssetMaterialContentReader.class);
        var tasks = mock(TaskService.class);
        var chunks = mock(MaterialChunkStore.class);
        var chunker = mock(MaterialChunker.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var queued = task("task_1", TaskLifecycleStatus.QUEUED, now);
        var running = task("task_1", TaskLifecycleStatus.RUNNING, now);
        var material = new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.TXT, MaterialStatus.UPLOADED,
            "task_1", null, null, "legacy text", now, null);

        when(materials.find("mat_1")).thenReturn(Optional.of(material));
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.getTaskStatus("task_1")).thenReturn(queued);
        when(tasks.startTask(queued, "UPLOADED")).thenReturn(running);
        when(tasks.updateTask(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(running);
        when(reader.read(any(LearningMaterial.class))).thenReturn(Optional.of("asset text"));
        when(chunker.chunk(any(LearningMaterial.class))).thenReturn(List.of());

        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, chunks, legacyParser, chunker, noEmbeddingProvider(),
            Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks), new AsyncProcessingAdmissionGuard(true),
            null, null, reader
        );

        service.consumeQueuedMaterialImport("mat_1");

        verify(reader).read(any(LearningMaterial.class));
        verifyNoInteractions(legacyParser);
    }

    @Test
    void workerMarksImportFailedWhenOcrProcessingFailsAndNeverMakesTheMaterialReady() {
        var materials = mock(MaterialStore.class);
        var reader = mock(OriginalAssetMaterialContentReader.class);
        var tasks = mock(TaskService.class);
        var chunker = mock(MaterialChunker.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var queued = task("task_1", TaskLifecycleStatus.QUEUED, now);
        var running = task("task_1", TaskLifecycleStatus.RUNNING, now);
        var material = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.UPLOADED,
            "task_1", null, null, "", now, null);
        when(materials.find("mat_1")).thenReturn(Optional.of(material));
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.getTaskStatus("task_1")).thenReturn(queued);
        when(tasks.startTask(queued, "UPLOADED")).thenReturn(running);
        when(tasks.updateTask(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(running);
        when(reader.readDocument(any(LearningMaterial.class), any(String.class))).thenThrow(new IllegalStateException("OCR failed"));
        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, mock(MaterialChunkStore.class), mock(MaterialParser.class), chunker,
            noEmbeddingProvider(), Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks),
            new AsyncProcessingAdmissionGuard(true), null, null, reader
        );

        assertThatThrownBy(() -> service.consumeQueuedMaterialImport("mat_1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("OCR failed");
        var saved = ArgumentCaptor.forClass(LearningMaterial.class);
        verify(materials, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(LearningMaterial::status).contains(MaterialStatus.FAILED).doesNotContain(MaterialStatus.READY);
        verifyNoInteractions(chunker);
    }

    @Test
    void retriesReuseTheSamePersistedRevisionIdentifierForTheSameImportTask() {
        var materials = mock(MaterialStore.class);
        var reader = mock(OriginalAssetMaterialContentReader.class);
        var tasks = mock(TaskService.class);
        var chunks = mock(MaterialChunkStore.class);
        var chunker = mock(MaterialChunker.class);
        var revisions = mock(com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository.class);
        var blocks = mock(com.suilearn.api.persistence.repository.DocumentBlockJpaRepository.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var queued = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, null, null, now);
        var running = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.RUNNING, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, now, null, now);
        var material = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.UPLOADED,
            "task_1", null, null, "", now, null);
        var parsed = new OriginalAssetMaterialContentReader.ParsedOriginalDocument("Recovered OCR", List.of());

        when(materials.find("mat_1")).thenReturn(Optional.of(material));
        when(materials.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tasks.getTaskStatus("task_1")).thenReturn(queued);
        when(tasks.startTask(queued, "UPLOADED")).thenReturn(running);
        when(tasks.updateTask(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(running);
        when(reader.readDocument(any(LearningMaterial.class), any(String.class)))
            .thenThrow(new IllegalStateException("OCR failed"))
            .thenReturn(Optional.of(parsed));
        when(chunker.chunk(any(LearningMaterial.class))).thenReturn(List.of());
        when(revisions.countByMaterialId("mat_1")).thenReturn(0L);
        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, chunks, mock(MaterialParser.class), chunker,
            noEmbeddingProvider(), Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks),
            new AsyncProcessingAdmissionGuard(true), null, null, reader, revisions, blocks
        );

        assertThatThrownBy(() -> service.consumeQueuedMaterialImport("mat_1", "task_1"))
            .isInstanceOf(RuntimeException.class).hasMessageContaining("OCR failed");
        service.consumeQueuedMaterialImport("mat_1", "task_1");

        var revisionIds = ArgumentCaptor.forClass(String.class);
        verify(reader, org.mockito.Mockito.times(2)).readDocument(any(LearningMaterial.class), revisionIds.capture());
        assertThat(revisionIds.getAllValues()).containsOnly(revisionIds.getValue());
    }

    @Test
    void ignoresAnOlderTaskDeliveryAfterTheMaterialHasBeenAssignedToANewerTask() {
        var materials = mock(MaterialStore.class);
        var reader = mock(OriginalAssetMaterialContentReader.class);
        var tasks = mock(TaskService.class);
        var now = Instant.parse("2026-07-14T00:00:00Z");
        var materialForTaskB = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.UPLOADED,
            "task_b", null, null, "", now, null);
        var taskA = new TaskStatus("task_a", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, now, null, null, now);

        when(materials.find("mat_1")).thenReturn(Optional.of(materialForTaskB));
        when(tasks.getTaskStatus("task_a")).thenReturn(taskA);
        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, mock(MaterialChunkStore.class), mock(MaterialParser.class), mock(MaterialChunker.class),
            noEmbeddingProvider(), Clock.fixed(now, java.time.ZoneOffset.UTC), tasks, new TaskExecutor(tasks),
            new AsyncProcessingAdmissionGuard(true), null, null, reader
        );

        assertThat(service.consumeQueuedMaterialImport("mat_1", "task_a")).isSameAs(materialForTaskB);

        verifyNoInteractions(reader);
    }

    @Test
    void legacyReadyContentMigrationIsTransactionalAndIdempotent() throws Exception {
        Optional<Class<?>> migrator = legacyMigrator();
        assertThat(migrator).isPresent();
        if (migrator.isEmpty()) {
            return;
        }
        var migration = migrator.orElseThrow().getDeclaredMethod("migrateReadyLegacyMaterials");

        assertThat(migration.getAnnotation(Transactional.class)).isNotNull();
        assertThat(migration.getReturnType()).isEqualTo(int.class);
    }

    private static Optional<Class<?>> legacyMigrator() {
        try {
            return Optional.of(Class.forName("com.suilearn.api.material.application.LegacyMaterialRevisionMigrator"));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Class<?>> originalAssetBridge() {
        try {
            return Optional.of(Class.forName("com.suilearn.api.material.application.OriginalAssetMaterialContentReader"));
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    private static TaskStatus task(String id, TaskLifecycleStatus status, Instant now) {
        return new TaskStatus(id, TaskKind.MATERIAL_IMPORT, status, "kb_1", null, null, null, null,
            0, "UPLOADED", null, null, 0, null, now, null, null, now);
    }

    private static EmbeddingProvider noEmbeddingProvider() {
        return new EmbeddingProvider() {
            @Override public String model() { return "none"; }
            @Override public EmbeddingProvider.Embedding embed(String text) { throw new AssertionError("embedding must not run"); }
            @Override public boolean supportsEmbeddings() { return false; }
        };
    }
}

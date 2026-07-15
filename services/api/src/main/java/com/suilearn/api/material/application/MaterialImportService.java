package com.suilearn.api.material.application;

import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.document.DocumentParser;
import com.suilearn.api.material.document.LibreOfficePreviewAssetService;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaterialImportService {
    private final Clock clock;
    private final EmbeddingProvider embeddingProvider;
    private final KnowledgeBaseStore knowledgeBases;
    private final MaterialChunkStore materialChunks;
    private final MaterialChunker materialChunker;
    private final MaterialParser materialParser;
    private final MaterialStore materials;
    private final TaskExecutor taskExecutor;
    private final TaskOutboxSubmissionService taskOutboxSubmissionService;
    private final TaskService taskService;
    private final AsyncProcessingAdmissionGuard asyncAdmission;
    private final AssetPromotionCoordinator assetPromotionCoordinator;
    private final OriginalAssetMaterialContentReader originalAssetContentReader;
    private final DocumentRevisionJpaRepository documentRevisions;
    private final DocumentBlockJpaRepository documentBlocks;
    private final MaterialUploadValidator uploadValidator;
    private final LibreOfficePreviewAssetService previewAssets;
    private final MaterialImportFailurePersistence failurePersistence;

    @Autowired
    public MaterialImportService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        MaterialChunkStore materialChunks,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Clock clock,
        TaskService taskService,
        TaskExecutor taskExecutor,
        AsyncProcessingAdmissionGuard asyncAdmission,
        TaskOutboxSubmissionService taskOutboxSubmissionService,
        AssetPromotionCoordinator assetPromotionCoordinator,
        OriginalAssetMaterialContentReader originalAssetContentReader,
        DocumentRevisionJpaRepository documentRevisions,
        DocumentBlockJpaRepository documentBlocks,
        MaterialUploadValidator uploadValidator,
        LibreOfficePreviewAssetService previewAssets,
        MaterialImportFailurePersistence failurePersistence
    ) {
        this.clock = clock;
        this.embeddingProvider = embeddingProvider;
        this.knowledgeBases = knowledgeBases;
        this.materialChunks = materialChunks;
        this.materialChunker = materialChunker;
        this.materialParser = materialParser;
        this.materials = materials;
        this.taskExecutor = taskExecutor;
        this.taskService = taskService;
        this.asyncAdmission = asyncAdmission;
        this.taskOutboxSubmissionService = taskOutboxSubmissionService;
        this.assetPromotionCoordinator = assetPromotionCoordinator;
        this.originalAssetContentReader = originalAssetContentReader;
        this.documentRevisions = documentRevisions;
        this.documentBlocks = documentBlocks;
        this.uploadValidator = uploadValidator;
        this.previewAssets = previewAssets;
        this.failurePersistence = failurePersistence;
    }

    /**
     * Compatibility constructor for direct service callers while the JSON import endpoint is retired.
     * Spring uses the constructor that includes the durable outbox dependency.
     */
    public MaterialImportService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        MaterialChunkStore materialChunks,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Clock clock,
        TaskService taskService,
        TaskExecutor taskExecutor,
        AsyncProcessingAdmissionGuard asyncAdmission
    ) {
        this(knowledgeBases, materials, materialChunks, materialParser, materialChunker, embeddingProvider, clock,
            taskService, taskExecutor, asyncAdmission, null, null, null, null, null, null, null, null);
    }

    public MaterialImportService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        MaterialChunkStore materialChunks,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Clock clock,
        TaskService taskService,
        TaskExecutor taskExecutor,
        AsyncProcessingAdmissionGuard asyncAdmission,
        TaskOutboxSubmissionService taskOutboxSubmissionService,
        AssetPromotionCoordinator assetPromotionCoordinator,
        OriginalAssetMaterialContentReader originalAssetContentReader
    ) {
        this(knowledgeBases, materials, materialChunks, materialParser, materialChunker, embeddingProvider, clock,
            taskService, taskExecutor, asyncAdmission, taskOutboxSubmissionService, assetPromotionCoordinator,
            originalAssetContentReader, null, null, null, null, null);
    }

    public MaterialImportService(
        KnowledgeBaseStore knowledgeBases, MaterialStore materials, MaterialChunkStore materialChunks,
        MaterialParser materialParser, MaterialChunker materialChunker, EmbeddingProvider embeddingProvider, Clock clock,
        TaskService taskService, TaskExecutor taskExecutor, AsyncProcessingAdmissionGuard asyncAdmission,
        TaskOutboxSubmissionService taskOutboxSubmissionService, AssetPromotionCoordinator assetPromotionCoordinator,
        OriginalAssetMaterialContentReader originalAssetContentReader, DocumentRevisionJpaRepository documentRevisions,
        DocumentBlockJpaRepository documentBlocks
    ) {
        this(knowledgeBases, materials, materialChunks, materialParser, materialChunker, embeddingProvider, clock,
            taskService, taskExecutor, asyncAdmission, taskOutboxSubmissionService, assetPromotionCoordinator,
            originalAssetContentReader, documentRevisions, documentBlocks, null, null, null);
    }

    public MaterialImportService(
        KnowledgeBaseStore knowledgeBases, MaterialStore materials, MaterialChunkStore materialChunks,
        MaterialParser materialParser, MaterialChunker materialChunker, EmbeddingProvider embeddingProvider, Clock clock,
        TaskService taskService, TaskExecutor taskExecutor, AsyncProcessingAdmissionGuard asyncAdmission,
        TaskOutboxSubmissionService taskOutboxSubmissionService, AssetPromotionCoordinator assetPromotionCoordinator,
        OriginalAssetMaterialContentReader originalAssetContentReader, DocumentRevisionJpaRepository documentRevisions,
        DocumentBlockJpaRepository documentBlocks, MaterialUploadValidator uploadValidator
    ) {
        this(knowledgeBases, materials, materialChunks, materialParser, materialChunker, embeddingProvider, clock,
            taskService, taskExecutor, asyncAdmission, taskOutboxSubmissionService, assetPromotionCoordinator,
            originalAssetContentReader, documentRevisions, documentBlocks, uploadValidator, null, null);
    }

    /** Creates the material/task/outbox admission only; parsing is performed by {@link #consumeQueuedMaterialImport(String)}. */
    public LearningMaterial importMaterial(String knowledgeBaseId, ImportMaterialRequest request) {
        asyncAdmission.requireNewImportAdmission();
        requireKnowledgeBase(knowledgeBaseId);
        var materialId = newId("mat");
        var importTask = submitImportTask(knowledgeBaseId, materialId);
        var material = new LearningMaterial(
            materialId,
            knowledgeBaseId,
            request.title(),
            request.sourceType(),
            MaterialStatus.UPLOADED,
            importTask.id(),
            null,
            null,
            request.content(),
            clock.instant(),
            null
        );
        return materials.save(material);
    }

    /** Streams the original multipart asset into the existing staged/promotion lifecycle after durable admission. */
    public LearningMaterial importMultipartMaterial(
        String knowledgeBaseId, String title, String fileName, com.suilearn.api.model.MaterialSourceType sourceType, AssetUpload original
    ) {
        if (assetPromotionCoordinator == null) {
            throw new IllegalStateException("Original asset storage is unavailable");
        }
        if (uploadValidator == null) {
            var material = importMaterial(knowledgeBaseId, new ImportMaterialRequest(title, fileName, sourceType, ""));
            assetPromotionCoordinator.store(original, material.id(), "ORIGINAL");
            return material;
        }
        try (var validated = uploadValidator.validate(sourceType, original)) {
            var material = importMaterial(knowledgeBaseId, new ImportMaterialRequest(title, fileName, sourceType, ""));
            assetPromotionCoordinator.store(validated.openAssetUpload(), material.id(), "ORIGINAL");
            return material;
        }
    }

    /** Durably submits a fresh processing run; prior immutable revisions remain untouched. */
    public TaskStatus reprocessMaterial(String materialId) {
        asyncAdmission.requireNewImportAdmission();
        var material = materials.find(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        if (originalAssetContentReader == null || !originalAssetContentReader.hasOriginal(material)) {
            throw new LegacyMaterialReprocessConflict();
        }
        var task = submitReprocessTask(material);
        materials.save(new LearningMaterial(
            material.id(), material.knowledgeBaseId(), material.title(), material.sourceType(), MaterialStatus.UPLOADED,
            task.id(), material.embeddingTaskId(), null, material.content(), material.createdAt(), material.deletedAt(), material.currentRevisionId()
        ));
        return task;
    }

    /**
     * Worker entrypoint for the durable document-processing route.  It is intentionally not a Rabbit listener:
     * the broker adapter must invoke this method after its lifecycle and manual-ack boundary are established.
     */
    @Transactional
    public LearningMaterial consumeQueuedMaterialImport(String materialId) {
        var saved = materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        return finishQueuedMaterialImport(saved, taskService.getTaskStatus(saved.importTaskId()));
    }

    /** Binds a broker delivery to its persisted task so a delayed prior message cannot run a newer request. */
    @Transactional
    public LearningMaterial consumeQueuedMaterialImport(String materialId, String taskId) {
        var saved = materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        if (!taskId.equals(saved.importTaskId())) {
            return saved;
        }
        var importTask = taskService.getTaskStatus(taskId);
        if (!materialId.equals(importTask.materialId())) {
            throw new IllegalArgumentException("Task does not belong to material: " + materialId);
        }
        return finishQueuedMaterialImport(saved, importTask);
    }

    private LearningMaterial finishQueuedMaterialImport(LearningMaterial saved, TaskStatus importTask) {
        var outcome = processImport(saved, importTask);
        var processed = outcome.material();
        if (processed.status() == MaterialStatus.FAILED) {
            if (failurePersistence != null) {
                failurePersistence.persist(saved.id(), importTask.id(), processed.errorMessage());
            }
            if (outcome.failure() instanceof IllegalArgumentException permanentFailure) {
                throw new IllegalArgumentException(processed.errorMessage(), permanentFailure);
            }
            throw new RetryableMaterialImportException(processed.errorMessage());
        }
        return processed;
    }

    private static final class RetryableMaterialImportException extends RuntimeException {
        private RetryableMaterialImportException(String message) { super(message); }
    }

    private TaskStatus submitImportTask(String knowledgeBaseId, String materialId) {
        if (taskOutboxSubmissionService == null) {
            return taskService.createTask(TaskKind.MATERIAL_IMPORT, knowledgeBaseId, materialId, null, null, "UPLOADED");
        }
        return taskOutboxSubmissionService.submit(
            TaskKind.MATERIAL_IMPORT,
            knowledgeBaseId,
            materialId,
            null,
            null,
            "UPLOADED",
            "UPLOADED",
            "material-import:" + materialId,
            materialId
        );
    }

    private TaskStatus submitReprocessTask(LearningMaterial material) {
        if (taskOutboxSubmissionService == null) {
            return taskService.createTask(TaskKind.MATERIAL_REPROCESS, material.knowledgeBaseId(), material.id(), null, null, "REPROCESS");
        }
        return taskOutboxSubmissionService.submit(
            TaskKind.MATERIAL_REPROCESS, material.knowledgeBaseId(), material.id(), null, null, "REPROCESS", "REPROCESS",
            "material-reprocess:" + material.id() + ":" + UUID.randomUUID(), material.id()
        );
    }

    private ImportOutcome processImport(LearningMaterial saved, TaskStatus importTask) {
        var materialRef = new AtomicReference<>(saved);
        var embeddingTaskRef = new AtomicReference<TaskStatus>();
        var failureRef = new AtomicReference<RuntimeException>();
        var material = taskExecutor.runManagedTask(
            importTask,
            "UPLOADED",
            importExecution -> {
                var parsing = materials.save(withStatus(saved, MaterialStatus.PARSING));
                materialRef.set(parsing);
                importExecution.progress(20, "PARSING", parsing.id(), null);
                var revisionId = originalAssetContentReader == null ? null : reserveProcessingRevision(parsing, importTask);
                var parsedOriginal = originalAssetContentReader == null
                    ? java.util.Optional.<OriginalAssetMaterialContentReader.ParsedOriginalDocument>empty()
                    : originalAssetContentReader.readDocument(parsing, revisionId);
                var parsedContent = parsedOriginal.map(OriginalAssetMaterialContentReader.ParsedOriginalDocument::content)
                    .or(() -> originalAssetContentReader == null ? java.util.Optional.empty() : originalAssetContentReader.read(parsing))
                    .orElse(null);
                var parsed = parsedContent == null
                    ? materialParser.parse(new MaterialParser.ParseRequest(
                        parsing.title(), null, parsing.sourceType(), parsing.content()
                    ))
                    : new MaterialParser.ParsedMaterial(parsedContent);
                var chunking = materials.save(withContentAndStatus(
                    parsing,
                    parsed.content(),
                    MaterialStatus.CHUNKING
                ));
                var revised = persistFileImportRevision(chunking, parsedOriginal.orElse(null), revisionId,
                    "REPROCESS".equals(importTask.currentStep()) ? "REPROCESS" : "FILE_IMPORT");
                var materialForIndexing = revised == null ? chunking : revised;
                createOfficePreview(materialForIndexing);
                materialRef.set(materialForIndexing);
                importExecution.progress(45, "CHUNKING", materialForIndexing.id(), null);
                var chunks = materialChunker.chunk(materialForIndexing);
                if (!embeddingProvider.supportsEmbeddings()) {
                    materialChunks.replace(materialForIndexing.id(), chunks.stream().map(this::withoutEmbedding).toList());
                    var ready = materials.save(withStatus(materialForIndexing, MaterialStatus.READY));
                    materialRef.set(ready);
                    importExecution.succeed(
                        "READY",
                        new TaskResultRef("MATERIAL", ready.id(), null),
                        ready.id(),
                        null
                    );
                    return ready;
                }
                var embeddingTask = taskService.createTask(
                    TaskKind.EMBEDDING,
                    materialForIndexing.knowledgeBaseId(),
                    materialForIndexing.id(),
                    null,
                    embeddingProvider.model(),
                    "INDEXING"
                );
                embeddingTaskRef.set(embeddingTask);
                var ready = taskExecutor.runManagedTask(
                    embeddingTask,
                    "INDEXING",
                    embeddingExecution -> {
                        var indexing = materials.save(withEmbeddingTaskId(withStatus(materialForIndexing, MaterialStatus.INDEXING), embeddingExecution.current().id()));
                        materialRef.set(indexing);
                        materialChunks.replace(indexing.id(), chunks.stream().map(this::withEmbedding).toList());
                        var indexed = materials.save(withStatus(indexing, MaterialStatus.READY));
                        materialRef.set(indexed);
                        embeddingExecution.succeed(
                            "READY",
                            new TaskResultRef("MATERIAL_CHUNKS", indexed.id(), chunks.size()),
                            indexed.id(),
                            null
                        );
                        return indexed;
                    },
                    (embeddingExecution, exception) -> {
                        embeddingExecution.fail(
                            "EMBEDDING_FAILED",
                            safeErrorMessage(exception),
                            materialRef.get().id(),
                            null
                        );
                        var fallback = withEmbeddingTaskId(materialRef.get(), embeddingExecution.current().id());
                        materialChunks.replace(fallback.id(), chunks.stream().map(this::withoutEmbedding).toList());
                        var fallbackReady = materials.save(withStatus(fallback, MaterialStatus.READY));
                        materialRef.set(fallbackReady);
                        return fallbackReady;
                    }
                );
                importExecution.succeed(
                    "READY",
                    new TaskResultRef("MATERIAL", ready.id(), null),
                    ready.id(),
                    null
                );
                return ready;
            },
            (importExecution, exception) -> {
                failureRef.set(exception);
                var lastMaterial = materialRef.get();
                var failedMaterial = embeddingTaskRef.get() == null
                    ? lastMaterial
                    : withEmbeddingTaskId(lastMaterial, embeddingTaskRef.get().id());
                var failed = materials.save(withStatusAndError(
                    failedMaterial,
                    MaterialStatus.FAILED,
                    safeErrorMessage(exception)
                ));
                importExecution.fail(
                    "MATERIAL_IMPORT_FAILED",
                    safeErrorMessage(exception),
                    failed.id(),
                    null
                );
                return failed;
            }
        );
        return new ImportOutcome(material, failureRef.get());
    }

    private record ImportOutcome(LearningMaterial material, RuntimeException failure) {
    }

    private void requireKnowledgeBase(String knowledgeBaseId) {
        knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial withStatus(LearningMaterial material, MaterialStatus status) {
        return withContentAndStatus(material, material.content(), status);
    }

    private LearningMaterial withStatusAndError(LearningMaterial material, MaterialStatus status, String errorMessage) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            status,
            material.importTaskId(),
            material.embeddingTaskId(),
            errorMessage,
            material.content(),
            material.createdAt(),
            material.deletedAt(),
            material.currentRevisionId()
        );
    }

    private LearningMaterial withContentAndStatus(LearningMaterial material, String content, MaterialStatus status) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            status,
            material.importTaskId(),
            material.embeddingTaskId(),
            status == MaterialStatus.FAILED ? material.errorMessage() : null,
            content,
            material.createdAt(),
            material.deletedAt(),
            material.currentRevisionId()
        );
    }

    private LearningMaterial withEmbeddingTaskId(LearningMaterial material, String embeddingTaskId) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
            material.importTaskId(),
            embeddingTaskId,
            material.errorMessage(),
            material.content(),
            material.createdAt(),
            material.deletedAt(),
            material.currentRevisionId()
        );
    }

    private LearningMaterial persistFileImportRevision(
        LearningMaterial material, OriginalAssetMaterialContentReader.ParsedOriginalDocument original, String revisionId, String origin
    ) {
        if (original == null || documentRevisions == null || documentBlocks == null) {
            return null;
        }
        int revisionNumber = documentRevisions.findById(revisionId)
            .map(DocumentRevisionEntity::getRevisionNumber)
            .orElseGet(() -> Math.toIntExact(documentRevisions.countByMaterialId(material.id()) + 1));
        var revision = new DocumentRevisionEntity(
            revisionId,
            material.id(),
            revisionNumber,
            checksum(material.content()),
            origin,
            "parser-v1",
            clock.instant()
        );
        documentRevisions.save(revision);
        for (DocumentParser.Block block : original.blocks()) {
            documentBlocks.save(new DocumentBlockEntity(
                newId("block"), revisionId, block.order(), block.pageNumber(), block.sectionPath(), block.content()
            ));
        }
        return materials.save(new LearningMaterial(
            material.id(), material.knowledgeBaseId(), material.title(), material.sourceType(), material.status(),
            material.importTaskId(), material.embeddingTaskId(), material.errorMessage(), material.content(), material.createdAt(),
            material.deletedAt(), revisionId
        ));
    }

    /**
     * The durable task is the processing-run identity.  Reserving its revision before OCR gives every
     * retry the same page-operation key while keeping the revision hidden until it becomes current.
     */
    private String reserveProcessingRevision(LearningMaterial material, TaskStatus importTask) {
        String revisionId = "rev_" + importTask.id();
        if (documentRevisions == null) {
            return revisionId;
        }
        documentRevisions.findById(revisionId).orElseGet(() -> documentRevisions.save(new DocumentRevisionEntity(
            revisionId,
            material.id(),
            Math.toIntExact(documentRevisions.countByMaterialId(material.id()) + 1),
            checksum(material.content()),
            importTask.kind() == TaskKind.MATERIAL_REPROCESS ? "REPROCESS" : "FILE_IMPORT",
            "parser-v1",
            clock.instant()
        )));
        return revisionId;
    }

    private void createOfficePreview(LearningMaterial material) {
        if (previewAssets == null || originalAssetContentReader == null || material.currentRevisionId() == null
            || (material.sourceType() != com.suilearn.api.model.MaterialSourceType.DOC
                && material.sourceType() != com.suilearn.api.model.MaterialSourceType.DOCX)) return;
        originalAssetContentReader.withOriginalTempFile(material,
            original -> previewAssets.preview(original, material.id(), material.currentRevisionId()));
    }

    private String checksum(String content) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private MaterialChunk withEmbedding(MaterialChunk chunk) {
        var embedding = embeddingProvider.embed(chunk.content()).values();
        return new MaterialChunk(
            chunk.id(),
            chunk.knowledgeBaseId(),
            chunk.materialId(),
            chunk.content(),
            chunk.ordinal(),
            chunk.sourceRef(),
            embedding,
            EmbeddingStatus.READY,
            embeddingProvider.model(),
            embedding.size()
        );
    }

    private MaterialChunk withoutEmbedding(MaterialChunk chunk) {
        return new MaterialChunk(
            chunk.id(),
            chunk.knowledgeBaseId(),
            chunk.materialId(),
            chunk.content(),
            chunk.ordinal(),
            chunk.sourceRef(),
            null,
            EmbeddingStatus.TEXT_ONLY,
            null,
            null
        );
    }

    private String safeErrorMessage(RuntimeException exception) {
        var message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return truncate(message);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

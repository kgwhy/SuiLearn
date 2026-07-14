package com.suilearn.api.material.application;

import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
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
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class MaterialImportService {
    private final Clock clock;
    private final EmbeddingProvider embeddingProvider;
    private final KnowledgeBaseStore knowledgeBases;
    private final MaterialChunkStore materialChunks;
    private final MaterialChunker materialChunker;
    private final MaterialParser materialParser;
    private final MaterialStore materials;
    private final TaskExecutor taskExecutor;
    private final TaskService taskService;
    private final AsyncProcessingAdmissionGuard asyncAdmission;

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
    }

    public LearningMaterial importMaterial(String knowledgeBaseId, ImportMaterialRequest request) {
        asyncAdmission.requireNewImportAdmission();
        requireKnowledgeBase(knowledgeBaseId);
        var importTask = taskService.createTask(
            TaskKind.MATERIAL_IMPORT,
            knowledgeBaseId,
            null,
            null,
            null,
            "UPLOADED"
        );
        var material = new LearningMaterial(
            newId("mat"),
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
        var saved = materials.save(material);
        var materialRef = new AtomicReference<>(saved);
        var embeddingTaskRef = new AtomicReference<TaskStatus>();
        return taskExecutor.runManagedTask(
            importTask,
            "UPLOADED",
            importExecution -> {
                var parsing = materials.save(withStatus(saved, MaterialStatus.PARSING));
                materialRef.set(parsing);
                importExecution.progress(20, "PARSING", parsing.id(), null);
                var parsed = materialParser.parse(new MaterialParser.ParseRequest(
                    parsing.title(),
                    request.fileName(),
                    parsing.sourceType(),
                    parsing.content()
                ));
                var chunking = materials.save(withContentAndStatus(
                    parsing,
                    parsed.content(),
                    MaterialStatus.CHUNKING
                ));
                materialRef.set(chunking);
                importExecution.progress(45, "CHUNKING", chunking.id(), null);
                var chunks = materialChunker.chunk(chunking);
                if (!embeddingProvider.supportsEmbeddings()) {
                    materialChunks.replace(chunking.id(), chunks.stream().map(this::withoutEmbedding).toList());
                    var ready = materials.save(withStatus(chunking, MaterialStatus.READY));
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
                    knowledgeBaseId,
                    chunking.id(),
                    null,
                    embeddingProvider.model(),
                    "INDEXING"
                );
                embeddingTaskRef.set(embeddingTask);
                var ready = taskExecutor.runManagedTask(
                    embeddingTask,
                    "INDEXING",
                    embeddingExecution -> {
                        var indexing = materials.save(withEmbeddingTaskId(withStatus(chunking, MaterialStatus.INDEXING), embeddingExecution.current().id()));
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
            material.deletedAt()
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
            material.deletedAt()
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
            material.deletedAt()
        );
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

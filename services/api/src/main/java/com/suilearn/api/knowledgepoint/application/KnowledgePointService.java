package com.suilearn.api.knowledgepoint.application;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.material.application.RevisionEvidenceResolver;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointService {
    private static final int MAX_EXTRACTED_POINTS = 16;
    private static final int MAX_EVIDENCE_CHUNKS = 8;
    private static final String EXTRACTION_QUERY = "core knowledge point concept API principle interview";
    private static final String STEP_AI_EXTRACTED = "AI_EXTRACTED";
    private static final String STEP_AI_EXTRACTION_FAILED = "AI_EXTRACTION_FAILED";

    private final AiProvider aiProvider;
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialChunkStore materialChunks;
    private final MaterialStore materials;
    private final SuiLearnAiProperties properties;
    private final Retriever retriever;
    private final SourceService sourceService;
    private final TaskService taskService;
    private final TaskOutboxSubmissionService submissions;
    private final RevisionEvidenceResolver revisionEvidenceResolver;

    public KnowledgePointService(
        AiProvider aiProvider, KnowledgeBaseStore knowledgeBases, MaterialStore materials,
        MaterialChunkStore materialChunks, KnowledgePointStore knowledgePoints, SuiLearnAiProperties properties,
        Retriever retriever, TaskService taskService, SourceService sourceService
    ) {
        this(aiProvider, knowledgeBases, materials, materialChunks, knowledgePoints, properties, retriever, taskService, sourceService, null, null);
    }

    public KnowledgePointService(
        AiProvider aiProvider, KnowledgeBaseStore knowledgeBases, MaterialStore materials,
        MaterialChunkStore materialChunks, KnowledgePointStore knowledgePoints, SuiLearnAiProperties properties,
        Retriever retriever, TaskService taskService, SourceService sourceService, TaskOutboxSubmissionService submissions
    ) {
        this(aiProvider, knowledgeBases, materials, materialChunks, knowledgePoints, properties, retriever, taskService,
            sourceService, submissions, null);
    }

    @Autowired
    public KnowledgePointService(
        AiProvider aiProvider, KnowledgeBaseStore knowledgeBases, MaterialStore materials,
        MaterialChunkStore materialChunks, KnowledgePointStore knowledgePoints, SuiLearnAiProperties properties,
        Retriever retriever, TaskService taskService, SourceService sourceService, TaskOutboxSubmissionService submissions,
        RevisionEvidenceResolver revisionEvidenceResolver
    ) {
        this.aiProvider = aiProvider;
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materialChunks = materialChunks;
        this.materials = materials;
        this.properties = properties;
        this.retriever = retriever;
        this.sourceService = sourceService;
        this.taskService = taskService;
        this.submissions = submissions;
        this.revisionEvidenceResolver = revisionEvidenceResolver;
    }

    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId) {
        return extractKnowledgePoints(materialId, null);
    }

    /** Extracts against the current revision, or a caller-selected immutable revision. */
    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId, String revisionId) {
        var material = requireReadyMaterial(materialId);
        var evidence = revisionEvidence(material, revisionId);
        var task = taskService.startTask(taskService.createTask(
            TaskKind.KNOWLEDGE_POINT_EXTRACTION, material.knowledgeBaseId(), material.id(),
            chatConfigured() ? properties.providerType() : null, chatConfigured() ? properties.chatModel() : null, "EXTRACTING"
        ), "EXTRACTING");
        return executeGeneration(task, material, evidence);
    }

    /** Submits durable work only; AI execution is reserved for the message consumer. */
    public TaskStatus submitGeneration(String materialId) {
        return submitGeneration(materialId, null);
    }

    /** Freezes the evidence revision in the durable dispatch payload. */
    public TaskStatus submitGeneration(String materialId, String revisionId) {
        var material = requireReadyMaterial(materialId);
        var evidence = revisionEvidence(material, revisionId);
        if (submissions == null) {
            throw new IllegalStateException("Knowledge point generation submission is unavailable");
        }
        return submissions.submit(
            TaskKind.KNOWLEDGE_POINT_EXTRACTION, material.knowledgeBaseId(), material.id(),
            chatConfigured() ? properties.providerType() : null, chatConfigured() ? properties.chatModel() : null,
            "QUEUED", "GENERATING_KNOWLEDGE_POINTS", material.id(), evidence.revisionId() == null
                ? material.id() : "revision:" + evidence.revisionId()
        );
    }

    /** Consumer boundary for durable knowledge point generation. */
    public KnowledgePointExtractionResult consumeGeneration(String taskId, String materialId) {
        return consumeGeneration(taskId, materialId, null);
    }

    /** Consumer uses the revision frozen when the task was admitted. */
    public KnowledgePointExtractionResult consumeGeneration(String taskId, String materialId, String revisionId) {
        var task = taskService.getTaskStatus(taskId);
        if (task.kind() != TaskKind.KNOWLEDGE_POINT_EXTRACTION || !materialId.equals(task.materialId())) {
            throw new IllegalArgumentException("Knowledge point generation task does not match material");
        }
        var material = requireReadyMaterial(materialId);
        var evidence = revisionEvidence(material, revisionId);
        var running = task.status() == TaskLifecycleStatus.QUEUED ? taskService.startTask(task, "EXTRACTING") : task;
        return executeGeneration(running, material, evidence);
    }

    private KnowledgePointExtractionResult executeGeneration(TaskStatus task, LearningMaterial material, RevisionEvidence evidence) {
        if (!chatConfigured()) {
            return failedResult(task, material, "AI_NOT_CONFIGURED", "Chat AI is not configured");
        }

        List<AiProvider.GeneratedKnowledgePoint> generated;
        try {
            generated = extractStructuredPoints(material, evidence);
        } catch (SchemaValidationException exception) {
            return failedResult(task, material, "AI_STRUCTURED_OUTPUT_INVALID", safeErrorMessage(exception));
        } catch (RuntimeException exception) {
            taskService.updateTask(task, TaskLifecycleStatus.FAILED, 100, STEP_AI_EXTRACTION_FAILED, null,
                STEP_AI_EXTRACTION_FAILED, safeErrorMessage(exception), material.id(), null);
            throw exception;
        }

        var extracted = generated.stream().map(point -> structuredDraft(material, point, evidence)).toList();
        extracted.forEach(knowledgePoints::save);
        var finished = taskService.updateTask(task, TaskLifecycleStatus.SUCCEEDED, 100, STEP_AI_EXTRACTED,
            new TaskResultRef("KNOWLEDGE_POINTS", material.id(), extracted.size()), null, null, material.id(), null);
        return new KnowledgePointExtractionResult(finished.id(), finished, extracted);
    }

    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return knowledgePoints.list(knowledgeBaseId).stream().sorted(Comparator.comparing(KnowledgePoint::title)).toList();
    }

    public KnowledgePoint updateKnowledgePoint(String knowledgePointId, UpdateKnowledgePointRequest request) {
        var existing = requireKnowledgePoint(knowledgePointId);
        return knowledgePoints.save(new KnowledgePoint(existing.id(), existing.knowledgeBaseId(), request.name(), request.description(),
            existing.sourceMaterialId(), existing.sourceRefs(), request.name(), request.description(), existing.definition(),
            existing.principles(), existing.applicationScenarios(), existing.pitfalls(), existing.reviewStatus(),
            existing.sourceOutdated(), existing.legacy()));
    }

    public KnowledgePoint reviewKnowledgePoint(String knowledgePointId, KnowledgePointReviewStatus reviewStatus) {
        return switch (reviewStatus) {
            case CONFIRMED -> confirmKnowledgePoint(knowledgePointId);
            case REJECTED -> rejectKnowledgePoint(knowledgePointId);
            case ARCHIVED -> archiveKnowledgePoint(knowledgePointId);
            case DRAFT -> throw new IllegalArgumentException("Knowledge point review cannot transition to DRAFT");
        };
    }

    public KnowledgePoint confirmKnowledgePoint(String knowledgePointId) {
        var existing = requireKnowledgePoint(knowledgePointId);
        if (existing.reviewStatus() != KnowledgePointReviewStatus.DRAFT || existing.legacy() || existing.sourceOutdated()
            || !isComplete(existing) || !hasCurrentCitation(existing)) {
            throw new IllegalStateException("Only complete current non-legacy DRAFT knowledge points can be confirmed");
        }
        return saveWithReviewStatus(existing, KnowledgePointReviewStatus.CONFIRMED);
    }

    public KnowledgePoint rejectKnowledgePoint(String knowledgePointId) {
        var existing = requireKnowledgePoint(knowledgePointId);
        if (existing.reviewStatus() != KnowledgePointReviewStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT knowledge points can be rejected");
        }
        return saveWithReviewStatus(existing, KnowledgePointReviewStatus.REJECTED);
    }

    public KnowledgePoint archiveKnowledgePoint(String knowledgePointId) {
        var existing = requireKnowledgePoint(knowledgePointId);
        if (existing.reviewStatus() != KnowledgePointReviewStatus.CONFIRMED
            && existing.reviewStatus() != KnowledgePointReviewStatus.REJECTED) {
            throw new IllegalStateException("Only CONFIRMED or REJECTED knowledge points can be archived");
        }
        return saveWithReviewStatus(existing, KnowledgePointReviewStatus.ARCHIVED);
    }

    private KnowledgePoint saveWithReviewStatus(KnowledgePoint existing, KnowledgePointReviewStatus reviewStatus) {
        return knowledgePoints.save(new KnowledgePoint(existing.id(), existing.knowledgeBaseId(), existing.name(), existing.description(),
            existing.sourceMaterialId(), existing.sourceRefs(), existing.title(), existing.shortSummary(), existing.definition(),
            existing.principles(), existing.applicationScenarios(), existing.pitfalls(), reviewStatus,
            existing.sourceOutdated(), existing.legacy()));
    }

    public void deleteKnowledgePoint(String knowledgePointId) {
        requireKnowledgePoint(knowledgePointId);
        knowledgePoints.delete(knowledgePointId);
    }

    private List<AiProvider.GeneratedKnowledgePoint> extractStructuredPoints(
        LearningMaterial material, RevisionEvidence evidence
    ) {
        var prompt = new AiProvider.KnowledgePointExtractionPrompt(
            material.knowledgeBaseId(), material.id(), material.title(),
            evidence.chunks().stream().map(MaterialChunk::sourceRef).toList(), MAX_EXTRACTED_POINTS
        );
        var generated = invokeWithConfiguredRetries(() -> aiProvider.extractKnowledgePoints(prompt));
        try {
            validateSchema(generated, material, evidence);
            return generated.stream().limit(MAX_EXTRACTED_POINTS).toList();
        } catch (SchemaValidationException exception) {
            var repaired = aiProvider.repairKnowledgePointExtraction(prompt, List.of(exception.getMessage()));
            validateSchema(repaired, material, evidence);
            return repaired.stream().limit(MAX_EXTRACTED_POINTS).toList();
        }
    }

    private List<AiProvider.GeneratedKnowledgePoint> invokeWithConfiguredRetries(
        java.util.function.Supplier<List<AiProvider.GeneratedKnowledgePoint>> invocation
    ) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt <= properties.maxRetries(); attempt++) {
            try {
                return invocation.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new IllegalStateException("Configured chat AI failed to extract knowledge points: " + safeErrorMessage(lastFailure), lastFailure);
    }

    private void validateSchema(
        List<AiProvider.GeneratedKnowledgePoint> generated, LearningMaterial material, RevisionEvidence evidence
    ) {
        if (generated == null || generated.isEmpty() || generated.stream().anyMatch(point -> !isComplete(point))) {
            throw new SchemaValidationException("AI returned incomplete structured knowledge points");
        }
        if (generated.stream().anyMatch(point -> !hasOnlyWhitelistedCitations(point, material, evidence))) {
            throw new SchemaValidationException("AI returned citations outside the selected material evidence");
        }
    }

    private boolean hasOnlyWhitelistedCitations(
        AiProvider.GeneratedKnowledgePoint point, LearningMaterial material, RevisionEvidence evidence
    ) {
        return point.citations().stream().allMatch(ref -> material.id().equals(ref.materialId())
            && validCitation(ref) && isWhitelistedCitation(ref, evidence));
    }

    private boolean isComplete(AiProvider.GeneratedKnowledgePoint point) {
        return point != null && hasText(point.title()) && hasText(point.shortSummary()) && hasText(point.definition())
            && hasValues(point.principles()) && hasValues(point.applicationScenarios()) && hasValues(point.pitfalls())
            && point.citations() != null && !point.citations().isEmpty();
    }

    private boolean isComplete(KnowledgePoint point) {
        return hasText(point.title()) && hasText(point.shortSummary()) && hasText(point.definition())
            && hasValues(point.principles()) && hasValues(point.applicationScenarios()) && hasValues(point.pitfalls());
    }

    private KnowledgePoint structuredDraft(LearningMaterial material, AiProvider.GeneratedKnowledgePoint point, RevisionEvidence evidence) {
        var citations = point.citations().stream().filter(ref -> material.id().equals(ref.materialId()))
            .filter(this::validCitation).map(ref -> canonicalCitation(ref, evidence)).filter(Objects::nonNull).toList();
        if (citations.isEmpty()) {
            throw new SchemaValidationException("AI result requires a current versioned citation");
        }
        return new KnowledgePoint(newId("kp"), material.knowledgeBaseId(), point.title(), point.shortSummary(), material.id(), citations,
            point.title(), point.shortSummary(), point.definition(), point.principles(), point.applicationScenarios(), point.pitfalls(),
            KnowledgePointReviewStatus.DRAFT, false, false);
    }

    private com.suilearn.api.model.SourceRef canonicalCitation(
        com.suilearn.api.model.SourceRef citation, RevisionEvidence evidence
    ) {
        if (evidence.revisionId() == null) {
            return citation;
        }
        return evidence.chunks().stream().map(MaterialChunk::sourceRef).filter(allowed -> allowed != null
            && Objects.equals(citation.materialId(), allowed.materialId())
            && Objects.equals(citation.revisionId(), allowed.revisionId())
            && Objects.equals(citation.pageNumber(), allowed.pageNumber())
            && Objects.equals(citation.blockId(), allowed.blockId())
            && Objects.equals(citation.excerpt(), allowed.excerpt())).findFirst().orElse(null);
    }

    private KnowledgePointExtractionResult failedResult(TaskStatus task, LearningMaterial material, String code, String message) {
        var failed = taskService.updateTask(task, TaskLifecycleStatus.FAILED, 100, STEP_AI_EXTRACTION_FAILED,
            null, code, message, material.id(), null);
        return new KnowledgePointExtractionResult(failed.id(), failed, List.of());
    }

    private RevisionEvidence revisionEvidence(LearningMaterial material, String requestedRevisionId) {
        String revisionId = requestedRevisionId == null || requestedRevisionId.isBlank() ? material.currentRevisionId() : requestedRevisionId;
        boolean historicalRevision = requestedRevisionId != null && !requestedRevisionId.isBlank()
            && !requestedRevisionId.equals(material.currentRevisionId());
        var chunks = historicalRevision ? historicalRevisionEvidence(material, revisionId) : extractionEvidence(material, revisionId);
        if (revisionId != null && chunks.isEmpty()) {
            throw new IllegalStateException("Selected revision has no usable evidence: " + revisionId);
        }
        return new RevisionEvidence(revisionId, chunks);
    }

    private List<MaterialChunk> historicalRevisionEvidence(LearningMaterial material, String revisionId) {
        if (revisionEvidenceResolver == null) {
            throw new IllegalStateException("Immutable revision evidence resolution is unavailable");
        }
        return revisionEvidenceResolver.resolve(material, revisionId).stream().map(ref -> new MaterialChunk(
            ref.id(), material.knowledgeBaseId(), material.id(), ref.excerpt(), 0, ref, null,
            com.suilearn.api.model.EmbeddingStatus.TEXT_ONLY, null, null
        )).toList();
    }

    private List<MaterialChunk> extractionEvidence(LearningMaterial material, String revisionId) {
        var retrieved = retriever.retrieveEvidence(new Retriever.RetrievalRequest(EXTRACTION_QUERY, material.knowledgeBaseId(), material.id()),
            MAX_EVIDENCE_CHUNKS);
        var byId = new LinkedHashMap<String, MaterialChunk>();
        retrieved.stream().filter(chunk -> material.id().equals(chunk.materialId())).filter(chunk -> matchesRevision(chunk, revisionId))
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        materialChunks.listByMaterial(material.id()).stream().filter(chunk -> matchesRevision(chunk, revisionId)).limit(MAX_EVIDENCE_CHUNKS)
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        return byId.values().stream().limit(MAX_EVIDENCE_CHUNKS).toList();
    }

    private boolean matchesRevision(MaterialChunk chunk, String revisionId) {
        return revisionId == null || (chunk.sourceRef() != null && revisionId.equals(chunk.sourceRef().revisionId()));
    }

    private boolean isWhitelistedCitation(com.suilearn.api.model.SourceRef citation, RevisionEvidence evidence) {
        if (evidence.revisionId() == null) return true;
        return evidence.chunks().stream().map(MaterialChunk::sourceRef).anyMatch(allowed -> allowed != null
            && Objects.equals(citation.materialId(), allowed.materialId())
            && Objects.equals(citation.revisionId(), allowed.revisionId())
            && Objects.equals(citation.pageNumber(), allowed.pageNumber())
            && Objects.equals(citation.blockId(), allowed.blockId())
            && Objects.equals(citation.excerpt(), allowed.excerpt()));
    }

    private boolean validCitation(com.suilearn.api.model.SourceRef ref) {
        return ref != null && !ref.deleted() && hasText(ref.revisionId()) && (ref.pageNumber() != null || hasText(ref.blockId()));
    }

    private boolean hasCurrentCitation(KnowledgePoint point) {
        return point.sourceRefs() != null && point.sourceRefs().stream().anyMatch(ref -> validCitation(ref)
            && materials.find(ref.materialId()).map(material -> ref.revisionId().equals(material.currentRevisionId())).orElse(false));
    }

    private void requireKnowledgeBase(String knowledgeBaseId) {
        knowledgeBases.find(knowledgeBaseId).orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return materials.find(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private LearningMaterial requireReadyMaterial(String materialId) {
        var material = requireMaterial(materialId);
        if (material.status() != MaterialStatus.READY) {
            throw new IllegalArgumentException("Knowledge points can only be extracted from READY material: " + materialId
                + " is " + material.status());
        }
        return material;
    }

    private KnowledgePoint requireKnowledgePoint(String knowledgePointId) {
        return knowledgePoints.find(knowledgePointId).orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
    }

    private boolean chatConfigured() { return properties.hasOpenAiCompatibleChatConfiguration(); }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private boolean hasValues(List<String> values) { return values != null && !values.isEmpty() && values.stream().allMatch(this::hasText); }

    private String safeErrorMessage(RuntimeException exception) {
        if (exception == null) return "AI extraction failed";
        var message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() <= 160 ? message : message.substring(0, 160);
    }

    private String newId(String prefix) { return prefix + "_" + UUID.randomUUID().toString().replace("-", ""); }

    private static class SchemaValidationException extends IllegalStateException {
        private SchemaValidationException(String message) { super(message); }
    }

    private record RevisionEvidence(String revisionId, List<MaterialChunk> chunks) { }
}

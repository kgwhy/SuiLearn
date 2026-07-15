package com.suilearn.api.controller;

import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.GenerateKnowledgePointInterviewQuestionsRequest;
import com.suilearn.api.dto.GenerateKnowledgePointsRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.MaterialImportAcceptedResponse;
import com.suilearn.api.dto.TaskSubmissionResponse;
import com.suilearn.api.dto.RenameKnowledgeBaseRequest;
import com.suilearn.api.dto.SubmitAnswerRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.generation.application.KnowledgePointQuestionGenerationService;
import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.material.application.MaterialQueryService;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.AnswerRecord;
import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.MaterialDeletionResult;
import com.suilearn.api.model.MaterialDetail;
import com.suilearn.api.model.MaterialMetadata;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.QuestionSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2")
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgePointService knowledgePointService;
    private final MaterialImportService materialImportService;
    private final MaterialQueryService materialQueryService;
    private final KnowledgePointQuestionGenerationService questionGenerationService;

    public KnowledgeBaseController(
        KnowledgeBaseService knowledgeBaseService,
        MaterialImportService materialImportService,
        MaterialQueryService materialQueryService,
        KnowledgePointService knowledgePointService
    ) {
        this(knowledgeBaseService, materialImportService, materialQueryService, knowledgePointService, null);
    }

    @Autowired
    public KnowledgeBaseController(
        KnowledgeBaseService knowledgeBaseService,
        MaterialImportService materialImportService,
        MaterialQueryService materialQueryService,
        KnowledgePointService knowledgePointService,
        KnowledgePointQuestionGenerationService questionGenerationService
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.materialImportService = materialImportService;
        this.materialQueryService = materialQueryService;
        this.knowledgePointService = knowledgePointService;
        this.questionGenerationService = questionGenerationService;
    }

    @GetMapping("/knowledge-bases")
    List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseService.listKnowledgeBases();
    }

    @PostMapping("/knowledge-bases")
    KnowledgeBase createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return knowledgeBaseService.createKnowledgeBase(request);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}")
    KnowledgeBaseDetail getKnowledgeBase(@PathVariable String knowledgeBaseId) {
        return knowledgeBaseService.getKnowledgeBaseDetail(knowledgeBaseId);
    }

    @PatchMapping("/knowledge-bases/{knowledgeBaseId}")
    KnowledgeBase renameKnowledgeBase(
        @PathVariable String knowledgeBaseId,
        @Valid @RequestBody RenameKnowledgeBaseRequest request
    ) {
        return knowledgeBaseService.renameKnowledgeBase(knowledgeBaseId, request);
    }

    @DeleteMapping("/knowledge-bases/{knowledgeBaseId}")
    ResponseEntity<Void> deleteKnowledgeBase(@PathVariable String knowledgeBaseId) {
        knowledgeBaseService.deleteKnowledgeBase(knowledgeBaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/questions")
    List<QuestionSummary> listQuestions(@PathVariable String knowledgeBaseId) {
        return knowledgeBaseService.listQuestions(knowledgeBaseId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/statistics")
    KnowledgeBaseStatistics getStatistics(@PathVariable String knowledgeBaseId) {
        return knowledgeBaseService.getStatistics(knowledgeBaseId);
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/answers")
    AnswerRecord submitAnswer(
        @PathVariable String knowledgeBaseId,
        @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return knowledgeBaseService.submitAnswer(knowledgeBaseId, request);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/materials")
    List<MaterialMetadata> listMaterials(@PathVariable String knowledgeBaseId) {
        return materialQueryService.listMaterials(knowledgeBaseId).stream()
            .map(KnowledgeBaseController::toMaterialMetadata)
            .toList();
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/materials")
    @Deprecated(since = "2026-07", forRemoval = false)
    MaterialMetadata importMaterial(
        @PathVariable String knowledgeBaseId,
        @Valid @RequestBody ImportMaterialRequest request
    ) {
        return toMaterialMetadata(materialImportService.importMaterial(knowledgeBaseId, request));
    }

    @PostMapping(value = "/knowledge-bases/{knowledgeBaseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<MaterialImportAcceptedResponse> importMaterialFormData(
        @PathVariable String knowledgeBaseId,
        @RequestParam String title,
        @RequestParam(required = false) String fileName,
        @RequestParam MaterialSourceType sourceType,
        @RequestParam MultipartFile file
    ) throws IOException {
        var submittedFileName = fileName == null || fileName.isBlank() ? file.getOriginalFilename() : fileName;
        com.suilearn.api.model.LearningMaterial material;
        try (var stream = file.getInputStream()) {
            material = materialImportService.importMultipartMaterial(
                knowledgeBaseId,
                title,
                submittedFileName,
                sourceType,
                new AssetUpload(stream, submittedFileName, file.getContentType())
            );
        }
        String taskHref = "/api/v2/tasks/" + material.importTaskId();
        return ResponseEntity.accepted()
            .location(URI.create(taskHref))
            .body(new MaterialImportAcceptedResponse(
                material.importTaskId(),
                com.suilearn.api.model.TaskLifecycleStatus.QUEUED,
                taskHref,
                material.id(),
                "/api/v2/materials/" + material.id()
            ));
    }

    @GetMapping("/materials/{materialId}")
    MaterialDetail getMaterial(@PathVariable String materialId) {
        return materialQueryService.getMaterialDetail(materialId);
    }

    @DeleteMapping("/materials/{materialId}")
    MaterialDeletionResult deleteMaterial(
        @PathVariable String materialId,
        @RequestParam(required = false) DeletedMaterialSavedContentPolicy savedContentPolicy,
        @RequestParam(required = false) DeletedMaterialPendingContentPolicy pendingContentPolicy
    ) {
        return materialQueryService.deleteMaterial(materialId, savedContentPolicy, pendingContentPolicy);
    }

    @PostMapping("/materials/{materialId}/reprocess")
    ResponseEntity<TaskSubmissionResponse> reprocessMaterial(@PathVariable String materialId) {
        var task = materialImportService.reprocessMaterial(materialId);
        return ResponseEntity.accepted().body(new TaskSubmissionResponse(task.id(), task.status(), "/api/v2/tasks/" + task.id()));
    }

    @PostMapping("/materials/{materialId}/extract-knowledge-points")
    KnowledgePointExtractionResult extractKnowledgePoints(@PathVariable String materialId) {
        return knowledgePointService.extractKnowledgePoints(materialId);
    }

    @PostMapping("/materials/{materialId}/knowledge-point-generations")
    ResponseEntity<TaskSubmissionResponse> submitKnowledgePointGeneration(
        @PathVariable String materialId,
        @Valid @RequestBody(required = false) GenerateKnowledgePointsRequest request
    ) {
        var task = request == null || request.revisionId() == null || request.revisionId().isBlank()
            ? knowledgePointService.submitGeneration(materialId)
            : knowledgePointService.submitGeneration(materialId, request.revisionId());
        String taskHref = "/api/v2/tasks/" + task.id();
        return ResponseEntity.accepted().location(URI.create(taskHref))
            .body(new TaskSubmissionResponse(task.id(), task.status(), taskHref));
    }

    ResponseEntity<TaskSubmissionResponse> submitKnowledgePointGeneration(String materialId) {
        return submitKnowledgePointGeneration(materialId, null);
    }

    @PostMapping("/knowledge-points/{knowledgePointId}/interview-question-generations")
    ResponseEntity<TaskSubmissionResponse> submitKnowledgePointInterviewQuestions(
        @PathVariable String knowledgePointId,
        @Valid @RequestBody(required = false) GenerateKnowledgePointInterviewQuestionsRequest request
    ) {
        request = request == null
            ? new GenerateKnowledgePointInterviewQuestionsRequest(null, null, null)
            : request;
        if (questionGenerationService == null) {
            throw new IllegalStateException("Knowledge point interview question generation is unavailable");
        }
        var submitted = questionGenerationService.submit(knowledgePointId, request);
        String taskHref = "/api/v2/tasks/" + submitted.taskId();
        return ResponseEntity.accepted().location(URI.create(taskHref))
            .body(new TaskSubmissionResponse(submitted.taskId(), com.suilearn.api.model.TaskLifecycleStatus.QUEUED, taskHref));
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/knowledge-points")
    List<KnowledgePoint> listKnowledgePoints(@PathVariable String knowledgeBaseId) {
        return knowledgePointService.listKnowledgePoints(knowledgeBaseId);
    }

    @PatchMapping("/knowledge-points/{knowledgePointId}")
    KnowledgePoint updateKnowledgePoint(
        @PathVariable String knowledgePointId,
        @Valid @RequestBody UpdateKnowledgePointRequest request
    ) {
        return knowledgePointService.updateKnowledgePoint(knowledgePointId, request);
    }

    @PostMapping("/knowledge-points/{knowledgePointId}/confirm")
    KnowledgePoint confirmKnowledgePoint(@PathVariable String knowledgePointId) {
        return knowledgePointService.confirmKnowledgePoint(knowledgePointId);
    }

    @PostMapping("/knowledge-points/{knowledgePointId}/reject")
    KnowledgePoint rejectKnowledgePoint(@PathVariable String knowledgePointId) {
        return knowledgePointService.rejectKnowledgePoint(knowledgePointId);
    }

    @DeleteMapping("/knowledge-points/{knowledgePointId}")
    ResponseEntity<Void> deleteKnowledgePoint(@PathVariable String knowledgePointId) {
        knowledgePointService.deleteKnowledgePoint(knowledgePointId);
        return ResponseEntity.noContent().build();
    }

    private static MaterialMetadata toMaterialMetadata(com.suilearn.api.model.LearningMaterial material) {
        return new MaterialMetadata(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
            material.importTaskId(),
            material.embeddingTaskId(),
            material.errorMessage(),
            material.createdAt(),
            material.deletedAt()
        );
    }
}

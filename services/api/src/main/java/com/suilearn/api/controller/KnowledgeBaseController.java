package com.suilearn.api.controller;

import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.RenameKnowledgeBaseRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.MaterialDeletionResult;
import com.suilearn.api.model.MaterialDetail;
import com.suilearn.api.model.MaterialMetadata;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.service.SuiLearnV2Service;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final SuiLearnV2Service service;

    public KnowledgeBaseController(SuiLearnV2Service service) {
        this.service = service;
    }

    @GetMapping("/knowledge-bases")
    List<KnowledgeBase> listKnowledgeBases() {
        return service.listKnowledgeBases();
    }

    @PostMapping("/knowledge-bases")
    KnowledgeBase createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return service.createKnowledgeBase(request);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}")
    KnowledgeBaseDetail getKnowledgeBase(@PathVariable String knowledgeBaseId) {
        return service.getKnowledgeBaseDetail(knowledgeBaseId);
    }

    @PatchMapping("/knowledge-bases/{knowledgeBaseId}")
    KnowledgeBase renameKnowledgeBase(
        @PathVariable String knowledgeBaseId,
        @Valid @RequestBody RenameKnowledgeBaseRequest request
    ) {
        return service.renameKnowledgeBase(knowledgeBaseId, request);
    }

    @DeleteMapping("/knowledge-bases/{knowledgeBaseId}")
    ResponseEntity<Void> deleteKnowledgeBase(@PathVariable String knowledgeBaseId) {
        service.deleteKnowledgeBase(knowledgeBaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/questions")
    List<QuestionSummary> listQuestions(@PathVariable String knowledgeBaseId) {
        return service.listQuestions(knowledgeBaseId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/statistics")
    KnowledgeBaseStatistics getStatistics(@PathVariable String knowledgeBaseId) {
        return service.getStatistics(knowledgeBaseId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/materials")
    List<MaterialMetadata> listMaterials(@PathVariable String knowledgeBaseId) {
        return service.listMaterials(knowledgeBaseId).stream()
            .map(KnowledgeBaseController::toMaterialMetadata)
            .toList();
    }

    @PostMapping("/knowledge-bases/{knowledgeBaseId}/materials")
    MaterialMetadata importMaterial(
        @PathVariable String knowledgeBaseId,
        @Valid @RequestBody ImportMaterialRequest request
    ) {
        return toMaterialMetadata(service.importMaterial(knowledgeBaseId, request));
    }

    @PostMapping(value = "/knowledge-bases/{knowledgeBaseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MaterialMetadata importMaterialFormData(
        @PathVariable String knowledgeBaseId,
        @RequestParam String title,
        @RequestParam(required = false) String fileName,
        @RequestParam MaterialSourceType sourceType,
        @RequestParam String content,
        @RequestParam(required = false) MultipartFile file
    ) {
        return toMaterialMetadata(service.importMaterial(
            knowledgeBaseId,
            new ImportMaterialRequest(title, fileName, sourceType, content)
        ));
    }

    @GetMapping("/materials/{materialId}")
    MaterialDetail getMaterial(@PathVariable String materialId) {
        return service.getMaterialDetail(materialId);
    }

    @DeleteMapping("/materials/{materialId}")
    MaterialDeletionResult deleteMaterial(
        @PathVariable String materialId,
        @RequestParam(required = false) DeletedMaterialSavedContentPolicy savedContentPolicy,
        @RequestParam(required = false) DeletedMaterialPendingContentPolicy pendingContentPolicy
    ) {
        return service.deleteMaterial(materialId, savedContentPolicy, pendingContentPolicy);
    }

    @PostMapping("/materials/{materialId}/extract-knowledge-points")
    List<KnowledgePoint> extractKnowledgePoints(@PathVariable String materialId) {
        return service.extractKnowledgePoints(materialId);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/knowledge-points")
    List<KnowledgePoint> listKnowledgePoints(@PathVariable String knowledgeBaseId) {
        return service.listKnowledgePoints(knowledgeBaseId);
    }

    @PatchMapping("/knowledge-points/{knowledgePointId}")
    KnowledgePoint updateKnowledgePoint(
        @PathVariable String knowledgePointId,
        @Valid @RequestBody UpdateKnowledgePointRequest request
    ) {
        return service.updateKnowledgePoint(knowledgePointId, request);
    }

    @DeleteMapping("/knowledge-points/{knowledgePointId}")
    ResponseEntity<Void> deleteKnowledgePoint(@PathVariable String knowledgePointId) {
        service.deleteKnowledgePoint(knowledgePointId);
        return ResponseEntity.noContent().build();
    }

    private static MaterialMetadata toMaterialMetadata(com.suilearn.api.model.LearningMaterial material) {
        return new MaterialMetadata(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
            material.createdAt(),
            material.deletedAt()
        );
    }
}

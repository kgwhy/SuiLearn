package com.suilearn.api.knowledgepoint.application;

import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointService {
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialStore materials;
    private final SourceService sourceService;
    private final TaskService taskService;

    public KnowledgePointService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        KnowledgePointStore knowledgePoints,
        TaskService taskService,
        SourceService sourceService
    ) {
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materials = materials;
        this.sourceService = sourceService;
        this.taskService = taskService;
    }

    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId) {
        var material = requireMaterial(materialId);
        var task = taskService.startTask(taskService.createTask(
            TaskKind.KNOWLEDGE_POINT_EXTRACTION,
            material.knowledgeBaseId(),
            material.id(),
            null,
            null,
            "EXTRACTING"
        ), "EXTRACTING");
        var candidates = extractCandidateTerms(material.content());
        var extracted = candidates.stream()
            .map(term -> new KnowledgePoint(
                newId("kp"),
                material.knowledgeBaseId(),
                term,
                "从资料《" + material.title() + "》中提取的候选知识点。",
                material.id(),
                List.of(sourceService.materialSourceRef(material))
            ))
            .toList();
        extracted.forEach(knowledgePoints::save);
        var finished = taskService.updateTask(
            task,
            TaskLifecycleStatus.SUCCEEDED,
            100,
            "READY",
            new TaskResultRef("KNOWLEDGE_POINTS", material.id(), extracted.size()),
            null,
            null,
            material.id(),
            null
        );
        return new KnowledgePointExtractionResult(finished.id(), finished, extracted);
    }

    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return knowledgePoints.list(knowledgeBaseId).stream()
            .sorted(Comparator.comparing(KnowledgePoint::name))
            .toList();
    }

    public KnowledgePoint updateKnowledgePoint(String knowledgePointId, UpdateKnowledgePointRequest request) {
        var existing = requireKnowledgePoint(knowledgePointId);
        return knowledgePoints.save(new KnowledgePoint(
            existing.id(),
            existing.knowledgeBaseId(),
            request.name(),
            request.description(),
            existing.sourceMaterialId(),
            existing.sourceRefs()
        ));
    }

    public void deleteKnowledgePoint(String knowledgePointId) {
        requireKnowledgePoint(knowledgePointId);
        knowledgePoints.delete(knowledgePointId);
    }

    private void requireKnowledgeBase(String knowledgeBaseId) {
        knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private KnowledgePoint requireKnowledgePoint(String knowledgePointId) {
        return knowledgePoints.find(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
    }

    private List<String> extractCandidateTerms(String content) {
        return List.of(content.split("[,，。；;、\\s]+")).stream()
            .map(String::trim)
            .filter(term -> term.length() >= 2 && term.length() <= 32)
            .distinct()
            .limit(8)
            .toList();
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

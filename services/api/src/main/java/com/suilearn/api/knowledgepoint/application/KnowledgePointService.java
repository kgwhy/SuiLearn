package com.suilearn.api.knowledgepoint.application;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskService;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointService {
    private static final int MAX_EXTRACTED_POINTS = 16;
    private static final int MAX_EVIDENCE_CHUNKS = 8;
    private static final String EXTRACTION_QUERY = "核心知识点 概念 API 原理 面试重点";

    private final AiProvider aiProvider;
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialChunkStore materialChunks;
    private final MaterialStore materials;
    private final Retriever retriever;
    private final SourceService sourceService;
    private final TaskService taskService;

    public KnowledgePointService(
        AiProvider aiProvider,
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        MaterialChunkStore materialChunks,
        KnowledgePointStore knowledgePoints,
        Retriever retriever,
        TaskService taskService,
        SourceService sourceService
    ) {
        this.aiProvider = aiProvider;
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materialChunks = materialChunks;
        this.materials = materials;
        this.retriever = retriever;
        this.sourceService = sourceService;
        this.taskService = taskService;
    }

    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId) {
        var material = requireMaterial(materialId);
        if (material.status() != MaterialStatus.READY) {
            throw new IllegalArgumentException(
                "Knowledge points can only be extracted from READY material: " + materialId
                    + " is " + material.status()
            );
        }
        var task = taskService.startTask(taskService.createTask(
            TaskKind.KNOWLEDGE_POINT_EXTRACTION,
            material.knowledgeBaseId(),
            material.id(),
            null,
            null,
            "EXTRACTING"
        ), "EXTRACTING");
        var evidence = extractionEvidence(material);
        var candidates = extractCandidateTerms(material, evidence);
        var sourceRefs = evidence.isEmpty()
            ? List.of(sourceService.materialSourceRef(material))
            : evidence.stream().map(MaterialChunk::sourceRef).toList();
        var extracted = candidates.stream()
            .map(candidate -> new KnowledgePoint(
                newId("kp"),
                material.knowledgeBaseId(),
                candidate.name(),
                candidate.description(),
                material.id(),
                sourceRefs
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

    private List<ExtractedCandidate> extractCandidateTerms(LearningMaterial material, List<MaterialChunk> evidence) {
        var generated = aiProvider.extractKnowledgePoints(new AiProvider.KnowledgePointExtractionPrompt(
            material.knowledgeBaseId(),
            material.id(),
            material.title(),
            evidence.stream().map(MaterialChunk::sourceRef).toList(),
            MAX_EXTRACTED_POINTS
        ));
        var candidates = new LinkedHashMap<String, ExtractedCandidate>();
        if (generated != null) {
            for (var point : generated) {
                addCandidate(candidates, point.name(), point.description(), material.title());
            }
        }
        if (candidates.isEmpty()) {
            for (var term : KnowledgePointCandidateExtractor.extract(material.content())) {
                addCandidate(candidates, term, null, material.title());
            }
        }
        return candidates.values().stream().limit(MAX_EXTRACTED_POINTS).toList();
    }

    private List<MaterialChunk> extractionEvidence(LearningMaterial material) {
        var retrieved = retriever.retrieveEvidence(
            new Retriever.RetrievalRequest(EXTRACTION_QUERY, material.knowledgeBaseId(), material.id()),
            MAX_EVIDENCE_CHUNKS
        );
        var byId = new LinkedHashMap<String, MaterialChunk>();
        retrieved.stream()
            .filter(chunk -> material.id().equals(chunk.materialId()))
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        materialChunks.listByMaterial(material.id()).stream()
            .limit(MAX_EVIDENCE_CHUNKS)
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        return byId.values().stream().limit(MAX_EVIDENCE_CHUNKS).toList();
    }

    private void addCandidate(
        LinkedHashMap<String, ExtractedCandidate> candidates,
        String rawName,
        String rawDescription,
        String materialTitle
    ) {
        if (candidates.size() >= MAX_EXTRACTED_POINTS) {
            return;
        }
        var name = KnowledgePointCandidateExtractor.sanitizeName(rawName);
        if (!KnowledgePointCandidateExtractor.isUsableName(name)) {
            return;
        }
        var description = rawDescription == null || rawDescription.isBlank()
            ? "基于资料《" + materialTitle + "》的证据片段提炼。"
            : rawDescription.trim();
        candidates.putIfAbsent(KnowledgePointCandidateExtractor.normalizeKey(name), new ExtractedCandidate(name, description));
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record ExtractedCandidate(String name, String description) {
    }
}

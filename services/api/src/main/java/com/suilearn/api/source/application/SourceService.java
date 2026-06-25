package com.suilearn.api.source.application;

import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SourceService {
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialChunkStore materialChunks;
    private final MaterialStore materials;

    public SourceService(
        KnowledgeBaseStore knowledgeBases,
        KnowledgePointStore knowledgePoints,
        MaterialStore materials,
        MaterialChunkStore materialChunks
    ) {
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materialChunks = materialChunks;
        this.materials = materials;
    }

    public List<SourceRef> normalize(String knowledgeBaseId, List<SourceRef> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceRef is required");
        }
        return sourceRefs.stream()
            .map(ref -> normalizeOne(knowledgeBaseId, ref))
            .toList();
    }

    public List<SourceRef> ensureUsable(List<SourceRef> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceRef is required");
        }
        for (var sourceRef : sourceRefs) {
            if (sourceRef == null || sourceRef.type() == null || sourceRef.id() == null || sourceRef.id().isBlank()) {
                throw new IllegalArgumentException("Invalid sourceRef");
            }
            if (sourceRef.deleted()) {
                throw new IllegalArgumentException("SourceRef is deleted: " + sourceRef.id());
            }
        }
        return sourceRefs;
    }

    public SourceRef materialSourceRef(LearningMaterial material) {
        return new SourceRef(
            SourceType.MATERIAL,
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            null,
            material.status() == MaterialStatus.DELETED,
            null
        );
    }

    public SourceRef chunkSourceRef(LearningMaterial material, String chunkId, String content) {
        return chunkSourceRef(material, chunkId, content, material.status() == MaterialStatus.DELETED);
    }

    public SourceRef chunkSourceRef(LearningMaterial material, String chunkId, String content, boolean deleted) {
        return new SourceRef(
            SourceType.MATERIAL_CHUNK,
            chunkId,
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            chunkId,
            deleted,
            truncate(content)
        );
    }

    public boolean referencesMaterial(List<SourceRef> sourceRefs, String materialId) {
        return sourceRefs != null && sourceRefs.stream().anyMatch(ref -> referencesMaterial(ref, materialId));
    }

    public boolean referencesMaterial(SourceRef sourceRef, String materialId) {
        return sourceRef != null
            && materialId != null
            && (materialId.equals(sourceRef.materialId())
                || (sourceRef.type() == SourceType.MATERIAL && materialId.equals(sourceRef.id())));
    }

    public List<SourceRef> markMaterialDeleted(List<SourceRef> sourceRefs, String materialId) {
        return sourceRefs.stream()
            .map(ref -> referencesMaterial(ref, materialId) ? markDeleted(ref) : ref)
            .toList();
    }

    public String firstMaterialId(List<SourceRef> sourceRefs) {
        if (sourceRefs == null) {
            return null;
        }
        return sourceRefs.stream()
            .map(ref -> ref.materialId() == null && ref.type() == SourceType.MATERIAL ? ref.id() : ref.materialId())
            .filter(id -> id != null && !id.isBlank())
            .findFirst()
            .orElse(null);
    }

    private SourceRef normalizeOne(String knowledgeBaseId, SourceRef ref) {
        if (ref == null || ref.type() == null || ref.id() == null || ref.id().isBlank()) {
            throw new IllegalArgumentException("Invalid sourceRef");
        }
        if (ref.type() == SourceType.KNOWLEDGE_BASE) {
            var kb = requireKnowledgeBase(ref.id());
            if (!kb.id().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return new SourceRef(ref.type(), ref.id(), kb.id(), kb.name(), null, null, false, ref.excerpt());
        }
        if (ref.type() == SourceType.KNOWLEDGE_POINT) {
            var point = requireKnowledgePoint(ref.id());
            if (!point.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return new SourceRef(
                ref.type(),
                point.id(),
                point.knowledgeBaseId(),
                point.name(),
                point.sourceMaterialId(),
                null,
                isMaterialDeleted(point.sourceMaterialId()),
                ref.excerpt()
            );
        }
        if (ref.type() == SourceType.MATERIAL) {
            var material = requireMaterial(ref.id());
            if (!material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return materialSourceRef(material);
        }
        if (ref.type() == SourceType.MATERIAL_CHUNK) {
            var chunk = requireChunk(ref.id());
            var material = requireMaterial(chunk.materialId());
            if (!material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return chunkSourceRef(
                material,
                chunk.id(),
                chunk.content(),
                material.status() == MaterialStatus.DELETED || !isUsableChunk(chunk)
            );
        }
        return new SourceRef(
            ref.type(),
            ref.id(),
            valueOrDefault(ref.knowledgeBaseId(), knowledgeBaseId),
            ref.title(),
            ref.materialId(),
            ref.chunkId(),
            ref.deleted(),
            ref.excerpt()
        );
    }

    private SourceRef markDeleted(SourceRef sourceRef) {
        return new SourceRef(
            sourceRef.type(),
            sourceRef.id(),
            sourceRef.knowledgeBaseId(),
            sourceRef.title(),
            sourceRef.materialId(),
            sourceRef.chunkId(),
            true,
            sourceRef.excerpt()
        );
    }

    private KnowledgeBase requireKnowledgeBase(String knowledgeBaseId) {
        return knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private KnowledgePoint requireKnowledgePoint(String knowledgePointId) {
        return knowledgePoints.find(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private MaterialChunk requireChunk(String chunkId) {
        return materialChunks.find(chunkId)
            .orElseThrow(() -> new IllegalArgumentException("Material chunk not found: " + chunkId));
    }

    private boolean isMaterialDeleted(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return false;
        }
        return materials.find(materialId)
            .map(material -> material.status() == MaterialStatus.DELETED)
            .orElse(false);
    }

    private boolean isUsableChunk(MaterialChunk chunk) {
        return chunk.embeddingStatus() == EmbeddingStatus.READY
            || chunk.embeddingStatus() == EmbeddingStatus.TEXT_ONLY;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

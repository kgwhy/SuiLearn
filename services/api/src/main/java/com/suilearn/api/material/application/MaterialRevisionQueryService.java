package com.suilearn.api.material.application;

import com.suilearn.api.dto.DocumentBlockResponse;
import com.suilearn.api.dto.DocumentRevisionResponse;
import com.suilearn.api.dto.MaterialReadingResponse;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MaterialRevisionQueryService {
    private final MaterialStore materials;
    private final DocumentRevisionJpaRepository revisions;
    private final DocumentBlockJpaRepository blocks;

    public MaterialRevisionQueryService(MaterialStore materials, DocumentRevisionJpaRepository revisions,
                                        DocumentBlockJpaRepository blocks) {
        this.materials = materials;
        this.revisions = revisions;
        this.blocks = blocks;
    }

    public DocumentRevisionResponse currentRevision(String materialId) {
        var material = materials.find(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        if (material.currentRevisionId() == null || material.currentRevisionId().isBlank()) {
            throw new IllegalArgumentException("Revision not found: current");
        }
        return revision(materialId, material.currentRevisionId());
    }

    public DocumentRevisionResponse revision(String materialId, String revisionId) {
        requireMaterial(materialId);
        var revision = revisions.findByIdAndMaterialId(revisionId, materialId)
            .orElseThrow(() -> new IllegalArgumentException("Revision not found: " + revisionId));
        var documentBlocks = blocks.findByRevisionIdOrderByBlockOrder(revisionId).stream().map(block ->
            new DocumentBlockResponse(block.getId(), block.getRevisionId(), block.getBlockOrder(), splitPath(block.getSectionPath()),
                block.getPageNumber(), block.getContent())
        ).toList();
        return toResponse(revision, documentBlocks);
    }

    public MaterialReadingResponse reading(String materialId, String revisionId) {
        var revision = revisionId == null || revisionId.isBlank() ? currentRevision(materialId) : revision(materialId, revisionId);
        return new MaterialReadingResponse(materialId, revision.id(), revision.origin(), "text/plain",
            revision.blocks().stream().map(DocumentBlockResponse::content).collect(java.util.stream.Collectors.joining("\n\n")),
            revision.blocks());
    }

    private void requireMaterial(String materialId) {
        materials.find(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private DocumentRevisionResponse toResponse(DocumentRevisionEntity revision, List<DocumentBlockResponse> documentBlocks) {
        int pageCount = documentBlocks.stream().map(DocumentBlockResponse::pageNumber).filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue).max().orElse(0);
        return new DocumentRevisionResponse(revision.getId(), revision.getMaterialId(), revision.getOrigin(),
            revision.getProcessingVersion(), documentBlocks.size(), pageCount, revision.getCreatedAt(), documentBlocks);
    }

    private List<String> splitPath(String path) {
        return path == null || path.isBlank() ? List.of() : List.of(path.split(" > "));
    }
}

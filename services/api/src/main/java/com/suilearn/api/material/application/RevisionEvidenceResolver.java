package com.suilearn.api.material.application;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Resolves immutable revision blocks after a material's mutable chunks have been replaced. */
@Service
public class RevisionEvidenceResolver {
    private final DocumentBlockJpaRepository blocks;
    private final DocumentRevisionJpaRepository revisions;

    public RevisionEvidenceResolver(DocumentBlockJpaRepository blocks, DocumentRevisionJpaRepository revisions) {
        this.blocks = blocks;
        this.revisions = revisions;
    }

    public List<SourceRef> resolve(LearningMaterial material, String revisionId) {
        resolve(material.id(), revisionId);
        var evidence = blocks.findByRevisionIdOrderByBlockOrder(revisionId).stream()
            .map(block -> new SourceRef(SourceType.MATERIAL_CHUNK, block.getId(), material.knowledgeBaseId(), material.title(),
                material.id(), block.getId(), false, block.getContent(), revisionId, block.getPageNumber(), block.getId()))
            .toList();
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("Revision has no immutable document blocks: " + revisionId);
        }
        return evidence;
    }

    /** Verifies the immutable revision belongs to the material that requested its evidence. */
    public void resolve(String materialId, String revisionId) {
        revisions.findByIdAndMaterialId(revisionId, materialId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Revision does not belong to material: " + revisionId + " / " + materialId
            ));
    }
}

package com.suilearn.api.material.application;

import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import com.suilearn.api.persistence.repository.LearningMaterialJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Migrates readable pre-revision READY material exactly once and makes the immutable revision current. */
@Service
public class LegacyMaterialRevisionMigrator {
    private final DocumentBlockJpaRepository blocks;
    private final Clock clock;
    private final LearningMaterialJpaRepository materials;
    private final DocumentRevisionJpaRepository revisions;

    public LegacyMaterialRevisionMigrator(
        LearningMaterialJpaRepository materials,
        DocumentRevisionJpaRepository revisions,
        DocumentBlockJpaRepository blocks,
        Clock clock
    ) {
        this.materials = materials;
        this.revisions = revisions;
        this.blocks = blocks;
        this.clock = clock;
    }

    @Transactional
    public int migrateReadyLegacyMaterials() {
        var migrated = 0;
        for (var material : materials.findByStatusAndContentIsNotNull("READY")) {
            if (material.getContent().isBlank()) {
                continue;
            }
            if (revisions.existsByMaterialId(material.getId())) {
                if (material.getCurrentRevisionId() == null || material.getCurrentRevisionId().isBlank()) {
                    revisions.findFirstByMaterialIdOrderByRevisionNumberDesc(material.getId()).ifPresent(existing -> {
                        material.setCurrentRevisionId(existing.getId());
                        materials.save(material);
                    });
                }
                continue;
            }
            var revisionId = newId("rev");
            revisions.save(new DocumentRevisionEntity(
                revisionId, material.getId(), 1, checksum(material.getContent()), "LEGACY_TEXT_MIGRATION", "legacy-text-v1", clock.instant()
            ));
            blocks.save(new DocumentBlockEntity(
                newId("block"), revisionId, 0, null, "LEGACY_TEXT", material.getContent()
            ));
            material.setCurrentRevisionId(revisionId);
            materials.save(material);
            migrated++;
        }
        return migrated;
    }

    private String checksum(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to checksum legacy material", exception);
        }
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

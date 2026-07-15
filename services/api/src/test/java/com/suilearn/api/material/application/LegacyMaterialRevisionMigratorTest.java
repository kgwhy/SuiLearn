package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.entity.LearningMaterialEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import com.suilearn.api.persistence.repository.LearningMaterialJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LegacyMaterialRevisionMigratorTest {
    @Test
    void migratesLegacyTextToCurrentRevisionWithASeparateOriginAndProcessingVersion() {
        var materials = mock(LearningMaterialJpaRepository.class);
        var revisions = mock(DocumentRevisionJpaRepository.class);
        var blocks = mock(DocumentBlockJpaRepository.class);
        var legacy = new LearningMaterialEntity("mat_1", "kb_1", "Old", "TXT", "READY", null, null, null,
            "legacy content", Instant.EPOCH, null, null);
        when(materials.findByStatusAndContentIsNotNull("READY")).thenReturn(List.of(legacy));
        when(revisions.existsByMaterialId("mat_1")).thenReturn(false);
        var migrator = new LegacyMaterialRevisionMigrator(materials, revisions, blocks, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        migrator.migrateReadyLegacyMaterials();

        var revision = ArgumentCaptor.forClass(DocumentRevisionEntity.class);
        verify(revisions).save(revision.capture());
        assertThat(revision.getValue().getOrigin()).isEqualTo("LEGACY_TEXT_MIGRATION");
        assertThat(revision.getValue().getProcessingVersion()).isEqualTo("legacy-text-v1");
        assertThat(legacy.getCurrentRevisionId()).isEqualTo(revision.getValue().getId());
        verify(materials).save(legacy);
    }

    @Test
    void repairsTheCurrentRevisionReferenceForAnAlreadyMigratedLegacyMaterial() {
        var materials = mock(LearningMaterialJpaRepository.class);
        var revisions = mock(DocumentRevisionJpaRepository.class);
        var blocks = mock(DocumentBlockJpaRepository.class);
        var legacy = new LearningMaterialEntity("mat_1", "kb_1", "Old", "TXT", "READY", null, null, null,
            "legacy content", Instant.EPOCH, null, null);
        var existingRevision = new DocumentRevisionEntity("rev_1", "mat_1", 1, "sum", "LEGACY_TEXT_MIGRATION", "legacy-text-v1", Instant.EPOCH);
        when(materials.findByStatusAndContentIsNotNull("READY")).thenReturn(List.of(legacy));
        when(revisions.existsByMaterialId("mat_1")).thenReturn(true);
        when(revisions.findFirstByMaterialIdOrderByRevisionNumberDesc("mat_1")).thenReturn(java.util.Optional.of(existingRevision));

        var migrated = new LegacyMaterialRevisionMigrator(materials, revisions, blocks, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
            .migrateReadyLegacyMaterials();

        assertThat(migrated).isZero();
        assertThat(legacy.getCurrentRevisionId()).isEqualTo("rev_1");
        verify(materials).save(legacy);
    }
}

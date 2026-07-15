package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialRevisionQueryServiceTest {
    @Test
    void returnsCurrentRevisionAndReadingWithOrderedBlocks() {
        var materials = mock(MaterialStore.class);
        var revisions = mock(DocumentRevisionJpaRepository.class);
        var blocks = mock(DocumentBlockJpaRepository.class);
        when(materials.find("mat_1")).thenReturn(Optional.of(material()));
        when(revisions.findByIdAndMaterialId("rev_1", "mat_1")).thenReturn(Optional.of(revision()));
        when(blocks.findByRevisionIdOrderByBlockOrder("rev_1")).thenReturn(List.of(
            new DocumentBlockEntity("block_1", "rev_1", 0, 1, "Intro", "First"),
            new DocumentBlockEntity("block_2", "rev_1", 1, 2, "Intro > Detail", "Second")
        ));
        var service = new MaterialRevisionQueryService(materials, revisions, blocks);

        var current = service.currentRevision("mat_1");
        var reading = service.reading("mat_1", null);

        assertThat(current.id()).isEqualTo("rev_1");
        assertThat(current.blocks()).extracting(block -> block.ordinal(), block -> block.content())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(0, "First"), org.assertj.core.groups.Tuple.tuple(1, "Second"));
        assertThat(reading.content()).isEqualTo("First\n\nSecond");
        assertThat(reading.blocks().get(1).sectionPath()).containsExactly("Intro", "Detail");
    }

    @Test
    void refusesARevisionThatDoesNotBelongToTheMaterial() {
        var materials = mock(MaterialStore.class);
        var revisions = mock(DocumentRevisionJpaRepository.class);
        when(materials.find("mat_1")).thenReturn(Optional.of(material()));
        when(revisions.findByIdAndMaterialId("rev_other", "mat_1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new MaterialRevisionQueryService(materials, revisions, mock(DocumentBlockJpaRepository.class))
            .revision("mat_1", "rev_other"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Revision not found");
    }

    private static LearningMaterial material() {
        return new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.PDF, MaterialStatus.READY,
            "task_1", null, null, "", Instant.EPOCH, null, "rev_1");
    }

    private static DocumentRevisionEntity revision() {
        return new DocumentRevisionEntity("rev_1", "mat_1", 1, "checksum", "FILE_IMPORT", Instant.EPOCH);
    }
}

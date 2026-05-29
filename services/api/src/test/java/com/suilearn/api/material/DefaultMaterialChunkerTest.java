package com.suilearn.api.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DefaultMaterialChunkerTest {
    private final DefaultMaterialChunker chunker = new DefaultMaterialChunker();

    @Test
    void chunksMaterialByNonBlankLinesAndKeepsSourceRefs() {
        var material = new LearningMaterial(
            "mat_1",
            "kb_1",
            "HashMap Notes",
            MaterialSourceType.MARKDOWN,
            MaterialStatus.CHUNKING,
            "  HashMap uses buckets.  \n\n\nCollision handling uses linked lists.\n  ",
            Instant.parse("2026-05-25T00:00:00Z"),
            null
        );

        var chunks = chunker.chunk(material);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting("content")
            .containsExactly("HashMap uses buckets.", "Collision handling uses linked lists.");
        assertThat(chunks).extracting("ordinal").containsExactly(0, 1);
        assertThat(chunks)
            .allSatisfy(chunk -> {
                assertThat(chunk.materialId()).isEqualTo(material.id());
                assertThat(chunk.sourceRef().type().name()).isEqualTo("MATERIAL_CHUNK");
                assertThat(chunk.sourceRef().materialId()).isEqualTo(material.id());
                assertThat(chunk.sourceRef().knowledgeBaseId()).isEqualTo(material.knowledgeBaseId());
                assertThat(chunk.sourceRef().deleted()).isFalse();
            });
    }
}

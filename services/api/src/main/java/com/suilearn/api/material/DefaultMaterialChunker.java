package com.suilearn.api.material;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultMaterialChunker implements MaterialChunker {
    @Override
    public List<MaterialChunk> chunk(LearningMaterial material) {
        var chunks = new ArrayList<MaterialChunk>();
        for (var paragraph : material.content().split("\\R\\s*\\R|\\R")) {
            var content = paragraph.trim();
            if (!content.isBlank()) {
                chunks.add(newMaterialChunk(material, content, chunks.size()));
            }
        }
        return chunks;
    }

    private MaterialChunk newMaterialChunk(LearningMaterial material, String content, int ordinal) {
        var chunkId = newId("chunk");
        return new MaterialChunk(chunkId, material.id(), content, ordinal, new SourceRef(
            SourceType.MATERIAL_CHUNK,
            chunkId,
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            chunkId,
            material.status() == MaterialStatus.DELETED,
            truncate(content)
        ));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

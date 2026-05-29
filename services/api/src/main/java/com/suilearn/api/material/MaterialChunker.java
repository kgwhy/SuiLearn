package com.suilearn.api.material;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import java.util.List;

public interface MaterialChunker {
    List<MaterialChunk> chunk(LearningMaterial material);
}

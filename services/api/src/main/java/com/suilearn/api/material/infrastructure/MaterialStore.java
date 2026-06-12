package com.suilearn.api.material.infrastructure;

import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialStore {
    private final SuiLearnV2Store store;

    public MaterialStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<LearningMaterial> list(String knowledgeBaseId) {
        return store.listMaterials(knowledgeBaseId);
    }

    public Optional<LearningMaterial> find(String materialId) {
        return store.findMaterial(materialId);
    }

    public LearningMaterial save(LearningMaterial material) {
        return store.saveMaterial(material);
    }
}

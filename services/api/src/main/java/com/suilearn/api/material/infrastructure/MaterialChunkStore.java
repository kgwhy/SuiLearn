package com.suilearn.api.material.infrastructure;

import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialChunkStore {
    private final SuiLearnV2Store store;

    public MaterialChunkStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public void replace(String materialId, List<MaterialChunk> chunks) {
        store.saveChunks(materialId, chunks);
    }

    public List<MaterialChunk> listByMaterial(String materialId) {
        return store.listChunksByMaterial(materialId);
    }

    public Optional<MaterialChunk> find(String chunkId) {
        return store.findChunk(chunkId);
    }

    public int invalidateByMaterial(String materialId) {
        return store.invalidateChunksByMaterial(materialId);
    }
}

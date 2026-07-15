package com.suilearn.api.material.application;

import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/** Opens promoted original assets for API proxying without exposing object-store references. */
@Service
public class PrivateMaterialAssetService {
    private static final String ORIGINAL = "ORIGINAL";
    private static final String PROMOTED = "PROMOTED";

    private final MaterialStore materials;
    private final MaterialAssetJpaRepository assets;
    private final AssetStorage storage;

    public PrivateMaterialAssetService(MaterialStore materials, MaterialAssetJpaRepository assets, AssetStorage storage) {
        this.materials = materials;
        this.assets = assets;
        this.storage = storage;
    }

    public PrivateOriginal openOriginal(String materialId) {
        materials.find(materialId).orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        var asset = assets.findFirstByMaterialIdAndAssetTypeAndPromotionState(materialId, ORIGINAL, PROMOTED)
            .map(entity -> entity.toRecord())
            .orElseThrow(MaterialOriginalUnavailableException::new);
        return new PrivateOriginal(storage.openPrivate(asset.objectKey()), asset.originalFilename(), asset.mimeType());
    }

    public record PrivateOriginal(InputStream stream, String filename, String mimeType) {
    }
}

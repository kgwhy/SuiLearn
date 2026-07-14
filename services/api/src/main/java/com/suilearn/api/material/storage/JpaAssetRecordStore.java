package com.suilearn.api.material.storage;

import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import java.util.List;

public final class JpaAssetRecordStore implements AssetRecordStore {
    private final MaterialAssetJpaRepository assets;

    public JpaAssetRecordStore(MaterialAssetJpaRepository assets) { this.assets = assets; }

    @Override public StoredAssetRecord save(StoredAssetRecord asset) { assets.save(MaterialAssetEntity.from(asset)); return asset; }
    @Override public void remove(String assetId) { assets.deleteById(assetId); }
    @Override public List<StoredAssetRecord> pendingDeletion() { return assets.findByDeletionRequestedAtIsNotNull().stream().map(MaterialAssetEntity::toRecord).toList(); }
    @Override public List<StoredAssetRecord> pendingPromotions() {
        return assets.findByPromotionState(AssetPromotionState.PENDING.name()).stream().map(MaterialAssetEntity::toRecord).toList();
    }
    @Override public StoredAssetRecord markPromoted(String assetId) {
        var asset = assets.findById(assetId).orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        asset.markPromoted();
        assets.save(asset);
        return asset.toRecord();
    }
}

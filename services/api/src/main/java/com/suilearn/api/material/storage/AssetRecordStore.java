package com.suilearn.api.material.storage;

import java.util.List;

public interface AssetRecordStore {
    StoredAssetRecord save(StoredAssetRecord asset);
    void remove(String assetId);
    List<StoredAssetRecord> pendingDeletion();
    List<StoredAssetRecord> pendingPromotions();
    StoredAssetRecord markPromoted(String assetId);
}

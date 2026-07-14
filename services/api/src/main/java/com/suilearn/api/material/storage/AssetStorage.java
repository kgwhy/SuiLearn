package com.suilearn.api.material.storage;

import java.io.InputStream;
import java.time.Instant;

public interface AssetStorage {
    StagedAsset stage(AssetUpload upload);
    StoredAssetRecord promote(StagedAsset staged, String materialId, String assetType);
    InputStream openPrivate(String objectKey);
    void delete(String objectKey);
    int cleanupTemporaryBefore(Instant cutoff);
}

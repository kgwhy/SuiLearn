package com.suilearn.api.material.storage;

import java.util.UUID;

public record StoredAssetRecord(
    String id, String objectKey, String materialId, String assetType, String checksum, long sizeBytes, String revisionId, String mimeType,
    String temporaryKey, String plannedObjectKey, AssetPromotionState promotionState, String originalFilename
) {
    public StoredAssetRecord(
        String id, String objectKey, String materialId, String assetType, String checksum, long sizeBytes, String revisionId, String mimeType,
        String temporaryKey, String plannedObjectKey, AssetPromotionState promotionState
    ) {
        this(id, objectKey, materialId, assetType, checksum, sizeBytes, revisionId, mimeType,
            temporaryKey, plannedObjectKey, promotionState, null);
    }

    public StoredAssetRecord(String id, String objectKey, String materialId, String assetType, String checksum, long sizeBytes) {
        this(id, objectKey, materialId, assetType, checksum, sizeBytes, null, null, null, null, AssetPromotionState.PROMOTED, null);
    }

    public StoredAssetRecord(
        String id, String objectKey, String materialId, String assetType, String checksum, long sizeBytes, String revisionId, String mimeType
    ) {
        this(id, objectKey, materialId, assetType, checksum, sizeBytes, revisionId, mimeType, null, null, AssetPromotionState.PROMOTED, null);
    }

    static StoredAssetRecord promoted(String objectKey, String materialId, String assetType, String checksum, long sizeBytes) {
        return new StoredAssetRecord("asset_" + UUID.randomUUID().toString().replace("-", ""), objectKey, materialId, assetType, checksum, sizeBytes, null, null);
    }

    public static StoredAssetRecord pending(
        String id, String temporaryKey, String plannedObjectKey, String materialId, String assetType, String checksum, long sizeBytes, String mimeType
    ) {
        return pending(id, temporaryKey, plannedObjectKey, materialId, assetType, checksum, sizeBytes, mimeType, null);
    }

    public static StoredAssetRecord pending(
        String id, String temporaryKey, String plannedObjectKey, String materialId, String assetType, String checksum, long sizeBytes,
        String mimeType, String originalFilename
    ) {
        return new StoredAssetRecord(id, null, materialId, assetType, checksum, sizeBytes, null, mimeType,
            temporaryKey, plannedObjectKey, AssetPromotionState.PENDING, originalFilename);
    }

    StoredAssetRecord promoted() {
        if (promotionState != AssetPromotionState.PENDING || plannedObjectKey == null || temporaryKey == null) {
            throw new IllegalStateException("Only a staged asset can be promoted");
        }
        return new StoredAssetRecord(id, plannedObjectKey, materialId, assetType, checksum, sizeBytes, revisionId, mimeType,
            null, null, AssetPromotionState.PROMOTED, originalFilename);
    }
}

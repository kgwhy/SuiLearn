package com.suilearn.api.material.storage;

public final class AssetPromotionCoordinator {
    private final AssetStorage storage;
    private final AssetRecordStore records;

    public AssetPromotionCoordinator(AssetStorage storage, AssetRecordStore records) { this.storage = storage; this.records = records; }

    public StoredAssetRecord store(AssetUpload upload, String materialId, String assetType) {
        var staged = storage.stage(upload);
        var planned = StoredAssetRecord.pending(
            "asset_" + java.util.UUID.randomUUID().toString().replace("-", ""), staged.temporaryKey(),
            "assets/" + staged.temporaryKey().substring("tmp/".length()), materialId, assetType,
            staged.checksum(), staged.sizeBytes(), staged.mimeType()
        );
        try {
            records.save(planned);
        } catch (RuntimeException exception) {
            storage.delete(staged.temporaryKey());
            throw exception;
        }
        return promoteAndConfirm(planned);
    }

    public void recoverPendingPromotions() {
        records.pendingPromotions().forEach(this::promoteAndConfirm);
    }

    private StoredAssetRecord promoteAndConfirm(StoredAssetRecord pending) {
        if (pending.promotionState() != AssetPromotionState.PENDING || pending.temporaryKey() == null || pending.plannedObjectKey() == null) {
            throw new IllegalArgumentException("Asset promotion record is incomplete");
        }
        var staged = new StagedAsset(pending.temporaryKey(), pending.checksum(), pending.sizeBytes(), pending.mimeType());
        storage.promote(staged, pending.materialId(), pending.assetType());
        var confirmed = records.markPromoted(pending.id());
        storage.delete(pending.temporaryKey());
        return confirmed;
    }
}

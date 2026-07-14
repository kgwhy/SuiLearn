package com.suilearn.api.material.storage;

public final class AssetDeletionCleanupTask {
    private final AssetStorage storage;
    private final AssetRecordStore records;

    public AssetDeletionCleanupTask(AssetStorage storage, AssetRecordStore records) { this.storage = storage; this.records = records; }

    public void runOnce() {
        for (var asset : records.pendingDeletion()) {
            storage.delete(asset.objectKey());
            records.remove(asset.id());
        }
    }
}

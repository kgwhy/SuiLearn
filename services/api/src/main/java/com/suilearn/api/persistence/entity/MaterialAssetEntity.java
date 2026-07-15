package com.suilearn.api.persistence.entity;

import com.suilearn.api.material.storage.StoredAssetRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "material_assets", uniqueConstraints = @UniqueConstraint(columnNames = {"materialId", "assetType", "checksum"}))
public class MaterialAssetEntity {
    @Id private String id;
    private String materialId;
    private String revisionId;
    private String assetType;
    private String objectKey;
    private String mimeType;
    private String originalFilename;
    private Long sizeBytes;
    private String checksum;
    private String temporaryKey;
    private String plannedObjectKey;
    private String promotionState;
    private Instant createdAt;
    private Instant deletionRequestedAt;

    protected MaterialAssetEntity() { }

    public static MaterialAssetEntity from(StoredAssetRecord asset) {
        var entity = new MaterialAssetEntity();
        entity.id = asset.id();
        entity.materialId = asset.materialId();
        entity.revisionId = asset.revisionId();
        entity.assetType = asset.assetType();
        entity.objectKey = asset.objectKey();
        entity.mimeType = asset.mimeType();
        entity.originalFilename = asset.originalFilename();
        entity.checksum = asset.checksum();
        entity.sizeBytes = asset.sizeBytes();
        entity.temporaryKey = asset.temporaryKey();
        entity.plannedObjectKey = asset.plannedObjectKey();
        entity.promotionState = asset.promotionState().name();
        entity.createdAt = Instant.now();
        return entity;
    }

    public static MaterialAssetEntity deletionRequested(StoredAssetRecord asset) {
        var entity = from(asset);
        entity.deletionRequestedAt = Instant.now();
        return entity;
    }

    public StoredAssetRecord toRecord() {
        return new StoredAssetRecord(id, objectKey, materialId, assetType, checksum, sizeBytes == null ? 0 : sizeBytes, revisionId, mimeType,
            temporaryKey, plannedObjectKey, com.suilearn.api.material.storage.AssetPromotionState.valueOf(promotionState), originalFilename);
    }

    public void markPromoted() {
        if (!"PENDING".equals(promotionState) || plannedObjectKey == null || temporaryKey == null) {
            throw new IllegalStateException("Only a staged asset can be promoted");
        }
        this.objectKey = plannedObjectKey;
        temporaryKey = null;
        plannedObjectKey = null;
        promotionState = "PROMOTED";
    }
}

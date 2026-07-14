package com.suilearn.api.material.storage;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

public final class MinioAssetStorage implements AssetStorage {
    private final MinioObjectGateway gateway;
    private final String bucket;
    private final Clock clock;
    private final Supplier<String> keyGenerator;

    MinioAssetStorage(MinioObjectGateway gateway, String bucket, Clock clock, Supplier<String> keyGenerator) {
        this.gateway = gateway;
        this.bucket = bucket;
        this.clock = clock;
        this.keyGenerator = keyGenerator;
    }

    public static MinioAssetStorage usingMinio(MinioObjectGateway gateway, String bucket, Clock clock) {
        return new MinioAssetStorage(gateway, bucket, clock, () -> UUID.randomUUID().toString().replace("-", ""));
    }

    public void initializePrivateBucket() {
        if (gateway.bucketExists(bucket)) return;
        try {
            gateway.createPrivateBucket(bucket);
        } catch (RuntimeException exception) {
            if (!gateway.bucketExists(bucket)) throw exception;
        }
    }

    @Override
    public StagedAsset stage(AssetUpload upload) {
        try {
            initializePrivateBucket();
            var digest = MessageDigest.getInstance("SHA-256");
            var counted = new CountingInputStream(new DigestInputStream(upload.stream(), digest));
            var temporaryKey = "tmp/" + keyGenerator.get();
            gateway.putPrivate(bucket, temporaryKey, counted, upload.mimeType());
            return new StagedAsset(temporaryKey, HexFormat.of().formatHex(digest.digest()), counted.count(), upload.mimeType());
        } catch (Exception exception) {
            throw new IllegalStateException("Asset staging failed", exception);
        }
    }

    @Override
    public StoredAssetRecord promote(StagedAsset staged, String materialId, String assetType) {
        var permanentKey = "assets/" + staged.temporaryKey().substring("tmp/".length());
        gateway.copy(bucket, staged.temporaryKey(), permanentKey);
        return StoredAssetRecord.promoted(permanentKey, materialId, assetType, staged.checksum(), staged.sizeBytes());
    }

    @Override public InputStream openPrivate(String objectKey) { initializePrivateBucket(); return gateway.getPrivate(bucket, objectKey); }
    @Override public void delete(String objectKey) { gateway.delete(bucket, objectKey); }

    @Override
    public int cleanupTemporaryBefore(Instant cutoff) {
        var orphans = gateway.list(bucket, "tmp/").stream().filter(object -> object.lastModifiedAt().isBefore(cutoff)).toList();
        orphans.forEach(orphan -> gateway.delete(bucket, orphan.key()));
        return orphans.size();
    }

    private static final class CountingInputStream extends FilterInputStream {
        private long count;
        private CountingInputStream(InputStream in) { super(in); }
        @Override public int read() throws IOException { var value = super.read(); if (value >= 0) count++; return value; }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException { var read = super.read(bytes, offset, length); if (read > 0) count += read; return read; }
        long count() { return count; }
    }
}

package com.suilearn.api.material.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.springframework.scheduling.annotation.Scheduled;

class MinioAssetStorageTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void streamsToTemporaryPrivateObjectWithChecksumAndFilenameIndependentKey() {
        var gateway = new RecordingObjectGateway();
        var storage = new MinioAssetStorage(gateway, "assets", clock, () -> "random-key");
        var body = new ByteArrayInputStream("asset body".getBytes(StandardCharsets.UTF_8));

        var staged = storage.stage(new AssetUpload(body, "answer.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        assertThat(gateway.receivedStream).isNotNull();
        assertThat(staged.temporaryKey()).isEqualTo("tmp/random-key");
        assertThat(staged.temporaryKey()).doesNotContain("answer.docx");
        assertThat(staged.checksum()).isEqualTo("97702a55839da4ec5879c4eec4030c905e074b71d398cf763f40321de1e04c13");
        assertThat(staged.sizeBytes()).isEqualTo(10);
        assertThat(gateway.privateBuckets).containsOnly("assets");
    }

    @Test
    void promotesOnlyAfterPersistenceAndCompensatesTemporaryObjectWhenDatabaseSaveFails() {
        var gateway = new RecordingObjectGateway();
        var storage = new MinioAssetStorage(gateway, "assets", clock, () -> "random-key");
        var records = new RecordingAssetRecordStore();
        var coordinator = new AssetPromotionCoordinator(storage, records);

        var asset = coordinator.store(new AssetUpload(new ByteArrayInputStream(new byte[] {1}), "user.pdf", "application/pdf"), "mat_1", "ORIGINAL");

        assertThat(gateway.copies).containsExactly("tmp/random-key->assets/random-key");
        assertThat(gateway.deleted).contains("tmp/random-key");
        assertThat(asset.objectKey()).isEqualTo("assets/random-key");
        assertThat(asset.mimeType()).isEqualTo("application/pdf");

        records.failSaves = true;
        assertThatThrownBy(() -> coordinator.store(new AssetUpload(new ByteArrayInputStream(new byte[] {2}), "bad.pdf", "application/pdf"), "mat_2", "ORIGINAL"))
            .isInstanceOf(IllegalStateException.class);
        assertThat(gateway.deleted).contains("tmp/random-key");
    }

    @Test
    void retainsTheTemporaryObjectAndDurablyRecoversWhenTheProcessCrashesBeforePromotionIsConfirmed() {
        var gateway = new RecordingObjectGateway();
        var storage = new MinioAssetStorage(gateway, "assets", clock, () -> "random-key");
        var records = new RecordingAssetRecordStore();
        records.failPromotionConfirmation = true;
        var coordinator = new AssetPromotionCoordinator(storage, records);

        assertThatThrownBy(() -> coordinator.store(
            new AssetUpload(new ByteArrayInputStream(new byte[] {1}), "user.pdf", "application/pdf"), "mat_1", "ORIGINAL"
        )).isInstanceOf(IllegalStateException.class);

        assertThat(records.pendingPromotions()).singleElement().satisfies(asset -> {
            assertThat(asset.objectKey()).isNull();
            assertThat(asset.temporaryKey()).isEqualTo("tmp/random-key");
            assertThat(asset.plannedObjectKey()).isEqualTo("assets/random-key");
            assertThat(asset.promotionState()).isEqualTo(AssetPromotionState.PENDING);
        });
        assertThat(gateway.deleted).doesNotContain("tmp/random-key");

        records.failPromotionConfirmation = false;
        coordinator.recoverPendingPromotions();

        assertThat(records.pendingPromotions()).isEmpty();
        assertThat(records.promoted).singleElement().satisfies(asset -> {
            assertThat(asset.objectKey()).isEqualTo("assets/random-key");
            assertThat(asset.temporaryKey()).isNull();
            assertThat(asset.promotionState()).isEqualTo(AssetPromotionState.PROMOTED);
        });
        assertThat(gateway.deleted).contains("tmp/random-key");
    }

    @Test
    void readsPrivatelyCleansAgedTemporaryOrphansAndRetriesDeletionUntilMetadataIsRemoved() {
        var gateway = new RecordingObjectGateway();
        gateway.objects = List.of(new StoredObject("tmp/old", Instant.parse("2026-07-12T09:00:00Z")), new StoredObject("tmp/new", Instant.parse("2026-07-13T09:59:00Z")));
        var storage = new MinioAssetStorage(gateway, "assets", clock, () -> "random-key");
        var records = new RecordingAssetRecordStore();
        records.pendingDeletion = List.of(new StoredAssetRecord("asset_1", "assets/delete-me", "mat_1", "ORIGINAL", "checksum", 1));
        var deletion = new AssetDeletionCleanupTask(storage, records);

        assertThat(storage.openPrivate("assets/delete-me")).isSameAs(gateway.readStream);
        assertThat(storage.cleanupTemporaryBefore(Instant.parse("2026-07-13T09:00:00Z"))).isEqualTo(1);
        deletion.runOnce();

        assertThat(gateway.deleted).contains("tmp/old", "assets/delete-me");
        assertThat(records.removedIds).containsExactly("asset_1");
    }

    @Test
    void minioGatewayStreamsPrivatePutWithoutCreatingPublicUrl() throws Exception {
        var client = mock(MinioClient.class);
        var gateway = new MinioSdkObjectGateway(client);

        gateway.putPrivate("assets", "tmp/key", new ByteArrayInputStream(new byte[] {1, 2}), "application/pdf");

        verify(client).putObject(any(io.minio.PutObjectArgs.class));
    }

    @Test
    void initializesMissingPrivateBucketAndToleratesConcurrentCreator() {
        var gateway = new RecordingObjectGateway();
        gateway.bucketExists = false;
        gateway.appearsAfterCreateAttempt = true;
        var storage = new MinioAssetStorage(gateway, "assets", clock, () -> "random-key");

        storage.initializePrivateBucket();

        assertThat(gateway.createBucketAttempts).isEqualTo(1);
        assertThat(gateway.bucketExistsChecks).isEqualTo(2);
    }

    @Test
    void registersBackgroundLifecycleForBucketBootstrapAndCleanupBeforeFirstUpload() throws Exception {
        var lifecycle = Class.forName("com.suilearn.api.material.storage.AssetStorageLifecycleScheduler");

        assertThat(lifecycle.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName)
            .contains("bootstrapBucket", "cleanupTemporaryObjects", "cleanupDeletedAssets");
        assertThat(java.util.Arrays.stream(lifecycle.getDeclaredMethods())
            .filter(method -> method.getName().equals("cleanupTemporaryObjects"))
            .findFirst().orElseThrow().isAnnotationPresent(Scheduled.class)).isTrue();
    }

    private static final class RecordingObjectGateway implements MinioObjectGateway {
        InputStream receivedStream;
        InputStream readStream = new ByteArrayInputStream(new byte[] {9});
        List<String> privateBuckets = new ArrayList<>();
        List<String> copies = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        List<StoredObject> objects = List.of();
        boolean bucketExists = true;
        boolean appearsAfterCreateAttempt;
        int bucketExistsChecks;
        int createBucketAttempts;

        @Override public void putPrivate(String bucket, String key, InputStream stream, String contentType) {
            privateBuckets.add(bucket); receivedStream = stream;
            try { stream.transferTo(java.io.OutputStream.nullOutputStream()); } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
        }
        @Override public InputStream getPrivate(String bucket, String key) { return readStream; }
        @Override public void copy(String bucket, String sourceKey, String targetKey) { copies.add(sourceKey + "->" + targetKey); }
        @Override public void delete(String bucket, String key) { deleted.add(key); }
        @Override public List<StoredObject> list(String bucket, String prefix) { return objects; }
        @Override public boolean bucketExists(String bucket) { bucketExistsChecks++; return bucketExists; }
        @Override public void createPrivateBucket(String bucket) {
            createBucketAttempts++;
            if (appearsAfterCreateAttempt) { bucketExists = true; throw new IllegalStateException("already exists"); }
            bucketExists = true;
        }
    }

    private static final class RecordingAssetRecordStore implements AssetRecordStore {
        boolean failSaves;
        boolean failPromotionConfirmation;
        List<StoredAssetRecord> pendingDeletion = List.of();
        List<StoredAssetRecord> staged = new ArrayList<>();
        List<StoredAssetRecord> promoted = new ArrayList<>();
        List<String> removedIds = new ArrayList<>();
        @Override public StoredAssetRecord save(StoredAssetRecord asset) {
            if (failSaves) throw new IllegalStateException("database failed");
            staged.add(asset);
            return asset;
        }
        @Override public void remove(String assetId) { removedIds.add(assetId); }
        @Override public List<StoredAssetRecord> pendingDeletion() { return pendingDeletion; }
        @Override public List<StoredAssetRecord> pendingPromotions() {
            return staged.stream().filter(asset -> asset.promotionState() == AssetPromotionState.PENDING).toList();
        }
        @Override public StoredAssetRecord markPromoted(String assetId) {
            if (failPromotionConfirmation) throw new IllegalStateException("database confirmation failed");
            var pending = pendingPromotions().stream().filter(asset -> asset.id().equals(assetId)).findFirst().orElseThrow();
            staged.remove(pending);
            var confirmed = pending.promoted();
            promoted.add(confirmed);
            return confirmed;
        }
    }
}

package com.suilearn.api.material.storage;

import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class AssetStorageLifecycleScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AssetStorageLifecycleScheduler.class);
    private final MinioAssetStorage storage;
    private final AssetDeletionCleanupTask deletionCleanup;
    private final AssetPromotionCoordinator promotions;
    private final Clock clock;
    private final Duration temporaryRetention;

    public AssetStorageLifecycleScheduler(
        MinioAssetStorage storage, AssetDeletionCleanupTask deletionCleanup, AssetPromotionCoordinator promotions, Clock clock,
        @org.springframework.beans.factory.annotation.Value("${suilearn.minio.temporary-retention-ms:86400000}") long temporaryRetentionMs
    ) {
        this.storage = storage;
        this.deletionCleanup = deletionCleanup;
        this.promotions = promotions;
        this.clock = clock;
        this.temporaryRetention = Duration.ofMillis(temporaryRetentionMs);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}", initialDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}")
    public void bootstrapBucket() {
        try {
            storage.initializePrivateBucket();
            promotions.recoverPendingPromotions();
        } catch (RuntimeException exception) {
            logger.warn("MinIO bucket bootstrap deferred; processing health remains down until recovery", exception);
        }
    }

    @Scheduled(fixedDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}", initialDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}")
    public void cleanupTemporaryObjects() {
        try {
            storage.cleanupTemporaryBefore(clock.instant().minus(temporaryRetention));
        } catch (RuntimeException exception) {
            logger.warn("MinIO temporary cleanup deferred; processing health reports dependency availability");
        }
    }

    @Scheduled(fixedDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}", initialDelayString = "${suilearn.minio.cleanup-interval-ms:3600000}")
    public void cleanupDeletedAssets() {
        try {
            deletionCleanup.runOnce();
        } catch (RuntimeException exception) {
            logger.warn("MinIO deletion cleanup deferred; processing health reports dependency availability");
        }
    }
}

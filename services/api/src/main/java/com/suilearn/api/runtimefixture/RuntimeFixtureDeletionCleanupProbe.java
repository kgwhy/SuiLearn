package com.suilearn.api.runtimefixture;

import com.suilearn.api.material.storage.AssetDeletionCleanupTask;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import io.minio.errors.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Runs physical deletion cleanup only for an internally-created fixture object and returns opaque outcomes. */
@Service
@Profile("runtime-fixture")
public final class RuntimeFixtureDeletionCleanupProbe {
    private static final byte[] FIXTURE_BYTES = new byte[] {0};
    private final AssetPromotionCoordinator promotions;
    private final AssetDeletionCleanupTask cleanup;
    private final AssetStorage storage;
    private final MaterialAssetJpaRepository assets;

    public RuntimeFixtureDeletionCleanupProbe(
        AssetPromotionCoordinator promotions, AssetDeletionCleanupTask cleanup, AssetStorage storage, MaterialAssetJpaRepository assets
    ) {
        this.promotions = promotions;
        this.cleanup = cleanup;
        this.storage = storage;
        this.assets = assets;
    }

    public DeletionCleanupProbeResponse trigger() {
        var stored = promotions.store(
            new AssetUpload(new ByteArrayInputStream(FIXTURE_BYTES), "fixture.bin", "application/octet-stream"),
            "runtime-fixture-" + UUID.randomUUID(), "RUNTIME_FIXTURE"
        );
        assets.saveAndFlush(MaterialAssetEntity.deletionRequested(stored));
        cleanup.runOnce();

        boolean assetRecordDeleted = !assets.existsById(stored.id());
        boolean objectCleanupConfirmed = objectIsNoLongerReadable(stored.objectKey());
        return new DeletionCleanupProbeResponse(assetRecordDeleted, objectCleanupConfirmed);
    }

    private boolean objectIsNoLongerReadable(String objectKey) {
        try (InputStream stream = storage.openPrivate(objectKey)) {
            return false;
        } catch (RuntimeException exception) {
            return isExplicitNotFound(exception);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isExplicitNotFound(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ErrorResponseException minioError
                && minioError.errorResponse() != null
                && "NoSuchKey".equals(minioError.errorResponse().code())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record DeletionCleanupProbeResponse(boolean assetRecordDeleted, boolean objectCleanupConfirmed) { }
}

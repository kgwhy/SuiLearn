package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.storage.AssetDeletionCleanupTask;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.StoredAssetRecord;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class RuntimeFixtureDeletionCleanupProbeTest {
    @Test
    void createsAndCleansOnlyAnInternalFixtureAssetAndReturnsOnlyCleanupBooleans() {
        var promotions = mock(AssetPromotionCoordinator.class);
        var cleanup = mock(AssetDeletionCleanupTask.class);
        var storage = mock(AssetStorage.class);
        var assets = mock(MaterialAssetJpaRepository.class);
        var fixtureAsset = new StoredAssetRecord("asset_fixture", "assets/fixture", "material_fixture", "ORIGINAL", "checksum", 1);
        when(promotions.store(any(), anyString(), anyString())).thenReturn(fixtureAsset);
        when(assets.existsById("asset_fixture")).thenReturn(false);
        var notFound = mock(ErrorResponseException.class);
        when(notFound.errorResponse()).thenReturn(new ErrorResponse("NoSuchKey", "not found", null, null, null, null, null));
        when(storage.openPrivate("assets/fixture")).thenThrow(new IllegalStateException("MinIO read failed", notFound));
        var probe = new RuntimeFixtureDeletionCleanupProbe(promotions, cleanup, storage, assets);

        var response = probe.trigger();

        assertThat(response.assetRecordDeleted()).isTrue();
        assertThat(response.objectCleanupConfirmed()).isTrue();
        assertThat(response.getClass().getRecordComponents()).extracting(component -> component.getName())
            .containsExactly("assetRecordDeleted", "objectCleanupConfirmed");
        verify(promotions).store(any(), anyString(), anyString());
        verify(assets).saveAndFlush(any());
        verify(cleanup).runOnce();
        verify(storage).openPrivate("assets/fixture");
    }

    @Test
    void doesNotTreatAnUnexpectedStorageFailureAsProofOfDeletion() {
        var promotions = mock(AssetPromotionCoordinator.class);
        var cleanup = mock(AssetDeletionCleanupTask.class);
        var storage = mock(AssetStorage.class);
        var assets = mock(MaterialAssetJpaRepository.class);
        var fixtureAsset = new StoredAssetRecord("asset_fixture", "assets/fixture", "material_fixture", "ORIGINAL", "checksum", 1);
        when(promotions.store(any(), anyString(), anyString())).thenReturn(fixtureAsset);
        when(assets.existsById("asset_fixture")).thenReturn(false);
        when(storage.openPrivate("assets/fixture")).thenThrow(new IllegalStateException("MinIO read failed"));
        var probe = new RuntimeFixtureDeletionCleanupProbe(promotions, cleanup, storage, assets);

        var response = probe.trigger();

        assertThat(response.objectCleanupConfirmed()).isFalse();
    }

    @Test
    void treatsOnlyAnExplicitMinioNoSuchKeyAsProofOfDeletion() {
        var promotions = mock(AssetPromotionCoordinator.class);
        var cleanup = mock(AssetDeletionCleanupTask.class);
        var storage = mock(AssetStorage.class);
        var assets = mock(MaterialAssetJpaRepository.class);
        var fixtureAsset = new StoredAssetRecord("asset_fixture", "assets/fixture", "material_fixture", "ORIGINAL", "checksum", 1);
        when(promotions.store(any(), anyString(), anyString())).thenReturn(fixtureAsset);
        when(assets.existsById("asset_fixture")).thenReturn(false);
        var notFound = mock(ErrorResponseException.class);
        when(notFound.errorResponse()).thenReturn(new ErrorResponse("NoSuchKey", "not found", null, null, null, null, null));
        when(storage.openPrivate("assets/fixture")).thenThrow(new IllegalStateException("MinIO read failed", notFound));
        var probe = new RuntimeFixtureDeletionCleanupProbe(promotions, cleanup, storage, assets);

        var response = probe.trigger();

        assertThat(response.objectCleanupConfirmed()).isTrue();
    }
}

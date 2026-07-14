package com.suilearn.api.material.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class JpaAssetRecordStoreTest {
    @Test
    void persistsAssetReferencesAndExposesOnlyDeletionRequestedAssetsForCleanup() {
        var repository = mock(MaterialAssetJpaRepository.class);
        var store = new JpaAssetRecordStore(repository);
        var asset = new StoredAssetRecord("asset_1", "assets/key", "mat_1", "ORIGINAL", "checksum", 12);
        when(repository.findByDeletionRequestedAtIsNotNull()).thenReturn(List.of(MaterialAssetEntity.deletionRequested(asset)));

        assertThat(store.save(asset)).isEqualTo(asset);
        assertThat(store.pendingDeletion()).containsExactly(asset);
        store.remove("asset_1");

        verify(repository).save(any(MaterialAssetEntity.class));
        verify(repository).deleteById("asset_1");
    }

    @Test
    void persistsPendingPromotionWithoutAnAvailableFinalObjectReference() {
        var repository = mock(MaterialAssetJpaRepository.class);
        var store = new JpaAssetRecordStore(repository);
        var pending = StoredAssetRecord.pending("asset_1", "tmp/key", "assets/key", "mat_1", "ORIGINAL", "checksum", 12, "application/pdf");
        when(repository.findByPromotionState("PENDING")).thenReturn(List.of(MaterialAssetEntity.from(pending)));
        when(repository.findById("asset_1")).thenReturn(java.util.Optional.of(MaterialAssetEntity.from(pending)));

        assertThat(store.save(pending).objectKey()).isNull();
        assertThat(store.pendingPromotions()).containsExactly(pending);
        assertThat(store.markPromoted("asset_1").objectKey()).isEqualTo("assets/key");

        verify(repository, times(2)).save(any(MaterialAssetEntity.class));
    }
}

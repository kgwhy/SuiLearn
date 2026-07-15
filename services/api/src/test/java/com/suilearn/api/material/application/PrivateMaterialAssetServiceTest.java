package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.AssetPromotionState;
import com.suilearn.api.material.storage.StoredAssetRecord;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrivateMaterialAssetServiceTest {
    @Test
    void opensOnlyThePromotedOriginalThroughPrivateStorage() throws Exception {
        var materials = mock(MaterialStore.class);
        var assets = mock(MaterialAssetJpaRepository.class);
        var storage = mock(AssetStorage.class);
        when(materials.find("mat_1")).thenReturn(Optional.of(material()));
        when(assets.findFirstByMaterialIdAndAssetTypeAndPromotionState("mat_1", "ORIGINAL", "PROMOTED"))
            .thenReturn(Optional.of(MaterialAssetEntity.from(asset())));
        when(storage.openPrivate("private/object")).thenReturn(new ByteArrayInputStream("private content".getBytes()));

        var result = new PrivateMaterialAssetService(materials, assets, storage).openOriginal("mat_1");

        assertThat(result.filename()).isEqualTo("notes.pdf");
        assertThat(result.mimeType()).isEqualTo("application/pdf");
        assertThat(result.stream().readAllBytes()).isEqualTo("private content".getBytes());
        assertThat(PrivateMaterialAssetService.PrivateOriginal.class.getRecordComponents())
            .extracting(component -> component.getName()).doesNotContain("objectKey", "url", "credential");
    }

    @Test
    void reportsOriginalAsUnavailableWhenLegacyMaterialHasNoPromotedAsset() {
        var materials = mock(MaterialStore.class);
        var assets = mock(MaterialAssetJpaRepository.class);
        when(materials.find("mat_1")).thenReturn(Optional.of(material()));
        when(assets.findFirstByMaterialIdAndAssetTypeAndPromotionState("mat_1", "ORIGINAL", "PROMOTED"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new PrivateMaterialAssetService(materials, assets, mock(AssetStorage.class)).openOriginal("mat_1"))
            .isInstanceOf(MaterialOriginalUnavailableException.class)
            .hasMessage("The original file is unavailable for this legacy material.");
    }

    private static LearningMaterial material() {
        return new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.PDF, MaterialStatus.READY,
            "task_1", null, null, "", Instant.EPOCH, null, "rev_1");
    }

    private static StoredAssetRecord asset() {
        return new StoredAssetRecord("asset_1", "private/object", "mat_1", "ORIGINAL", "sum", 1, null,
            "application/pdf", null, null, AssetPromotionState.PROMOTED, "notes.pdf");
    }
}

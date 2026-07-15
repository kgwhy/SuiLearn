package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialAssetJpaRepository extends JpaRepository<MaterialAssetEntity, String> {
    List<MaterialAssetEntity> findByDeletionRequestedAtIsNotNull();
    List<MaterialAssetEntity> findByPromotionState(String promotionState);
    Optional<MaterialAssetEntity> findFirstByMaterialIdAndAssetTypeAndPromotionState(
        String materialId, String assetType, String promotionState
    );
}

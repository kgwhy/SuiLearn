package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.MaterialChunkEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialChunkJpaRepository extends JpaRepository<MaterialChunkEntity, String> {
    List<MaterialChunkEntity> findByMaterialId(String materialId);

    void deleteByMaterialId(String materialId);

    void deleteByMaterialIdIn(Collection<String> materialIds);
}

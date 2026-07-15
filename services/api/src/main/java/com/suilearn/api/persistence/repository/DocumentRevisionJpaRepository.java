package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRevisionJpaRepository extends JpaRepository<DocumentRevisionEntity, String> {
    boolean existsByMaterialId(String materialId);
    long countByMaterialId(String materialId);
    Optional<DocumentRevisionEntity> findFirstByMaterialIdOrderByRevisionNumberDesc(String materialId);
    Optional<DocumentRevisionEntity> findByIdAndMaterialId(String id, String materialId);
}

package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentBlockJpaRepository extends JpaRepository<DocumentBlockEntity, String> {
    List<DocumentBlockEntity> findByRevisionIdOrderByBlockOrder(String revisionId);
}

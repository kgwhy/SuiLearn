package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.KnowledgePointEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgePointJpaRepository extends JpaRepository<KnowledgePointEntity, String> {
    List<KnowledgePointEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

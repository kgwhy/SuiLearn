package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.LearningMaterialEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningMaterialJpaRepository extends JpaRepository<LearningMaterialEntity, String> {
    List<LearningMaterialEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.AiNoteDraftEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiNoteDraftJpaRepository extends JpaRepository<AiNoteDraftEntity, String> {
    List<AiNoteDraftEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

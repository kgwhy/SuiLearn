package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.GeneratedContentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedContentJpaRepository extends JpaRepository<GeneratedContentEntity, String> {
    List<GeneratedContentEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

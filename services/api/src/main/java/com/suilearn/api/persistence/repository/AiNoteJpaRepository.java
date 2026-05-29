package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.AiNoteEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiNoteJpaRepository extends JpaRepository<AiNoteEntity, String> {
    List<AiNoteEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

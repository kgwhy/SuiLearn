package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.TaskStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStatusJpaRepository extends JpaRepository<TaskStatusEntity, String> {
    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

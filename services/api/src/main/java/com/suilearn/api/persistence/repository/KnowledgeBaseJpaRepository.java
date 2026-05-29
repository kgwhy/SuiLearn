package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseJpaRepository extends JpaRepository<KnowledgeBaseEntity, String> {
}

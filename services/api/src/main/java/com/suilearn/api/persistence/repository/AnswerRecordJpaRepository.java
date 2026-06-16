package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.AnswerRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRecordJpaRepository extends JpaRepository<AnswerRecordEntity, String> {
    List<AnswerRecordEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    List<AnswerRecordEntity> findByQuestionId(String questionId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}

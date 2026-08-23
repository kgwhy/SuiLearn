package com.suilearn.api.rag.index;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexVersionRepository extends JpaRepository<IndexVersionEntity, String> {
    List<IndexVersionEntity> findByKnowledgeBaseIdOrderByVersionNoDesc(String knowledgeBaseId);
    Optional<IndexVersionEntity> findFirstByKnowledgeBaseIdAndSignatureAndReadyTrueOrderByVersionNoDesc(
        String knowledgeBaseId, String signature);
}

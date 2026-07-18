package com.suilearn.api.agent.infrastructure.memory;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSemanticMemoryJpaRepository extends JpaRepository<AgentSemanticMemoryEntity, String> {
    List<AgentSemanticMemoryEntity> findByLearnerIdAndMemoryTypeIn(String learnerId, Collection<String> memoryTypes);

    long deleteByLearnerId(String learnerId);
}

package com.suilearn.api.agent.memory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryConsolidationCommandRepository extends JpaRepository<MemoryConsolidationCommandEntity, String> {
    Optional<MemoryConsolidationCommandEntity> findByIdempotencyKey(String idempotencyKey);
    List<MemoryConsolidationCommandEntity> findTop10ByStatusOrderByCreatedAtAsc(String status);
}

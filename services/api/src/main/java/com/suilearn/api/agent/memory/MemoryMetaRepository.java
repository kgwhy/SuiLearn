package com.suilearn.api.agent.memory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryMetaRepository extends JpaRepository<MemoryMetaEntity, String> {
    Optional<MemoryMetaEntity> findByLearnerIdAndSurface(String learnerId, String surface);
}

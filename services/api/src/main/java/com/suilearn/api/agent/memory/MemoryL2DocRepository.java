package com.suilearn.api.agent.memory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryL2DocRepository extends JpaRepository<MemoryL2DocEntity, String> {
    List<MemoryL2DocEntity> findByLearnerIdOrderByUpdatedAtDesc(String learnerId);
    Optional<MemoryL2DocEntity> findByLearnerIdAndSurface(String learnerId, String surface);
}

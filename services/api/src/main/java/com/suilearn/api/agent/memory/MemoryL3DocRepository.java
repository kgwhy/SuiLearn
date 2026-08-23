package com.suilearn.api.agent.memory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryL3DocRepository extends JpaRepository<MemoryL3DocEntity, String> {
    List<MemoryL3DocEntity> findByLearnerIdOrderBySlotAsc(String learnerId);
}

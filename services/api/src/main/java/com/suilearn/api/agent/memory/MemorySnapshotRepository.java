package com.suilearn.api.agent.memory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemorySnapshotRepository extends JpaRepository<MemorySnapshotEntity, String> {
    List<MemorySnapshotEntity> findByLearnerIdAndConsumedFalseOrderByCreatedAtAsc(String learnerId);
    boolean existsByEntityKeyAndFingerprint(String entityKey, String fingerprint);
}

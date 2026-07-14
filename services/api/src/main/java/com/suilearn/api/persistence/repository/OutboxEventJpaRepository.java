package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findByStateInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(List<String> states, Instant now);
    List<OutboxEventEntity> findByStateOrderByCreatedAtAsc(String state);
}

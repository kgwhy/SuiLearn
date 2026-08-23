package com.suilearn.api.agent.infrastructure.turn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnJpaRepository extends JpaRepository<TurnEntity, String> {
    Optional<TurnEntity> findFirstBySessionIdAndStatusInOrderByCreatedAtDesc(
        String sessionId, Collection<String> statuses);

    List<TurnEntity> findByStatus(String status);
}

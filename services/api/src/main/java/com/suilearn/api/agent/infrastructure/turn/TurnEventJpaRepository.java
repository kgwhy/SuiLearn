package com.suilearn.api.agent.infrastructure.turn;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnEventJpaRepository extends JpaRepository<TurnEventEntity, TurnEventId> {
    List<TurnEventEntity> findByIdTurnIdAndIdSeqGreaterThanOrderByIdSeqAsc(String turnId, long afterSeq);

    Optional<TurnEventEntity> findFirstByIdTurnIdOrderByIdSeqDesc(String turnId);

    long countByIdTurnId(String turnId);
}

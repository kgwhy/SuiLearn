package com.suilearn.api.agent.infrastructure.turn;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSummaryJpaRepository extends JpaRepository<SessionSummaryEntity, String> {
}

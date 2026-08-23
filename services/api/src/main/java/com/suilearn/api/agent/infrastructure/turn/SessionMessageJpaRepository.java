package com.suilearn.api.agent.infrastructure.turn;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionMessageJpaRepository extends JpaRepository<SessionMessageEntity, String> {
}

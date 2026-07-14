package com.suilearn.api.persistence.repository;

import com.suilearn.api.persistence.entity.DeadLetterMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterMessageJpaRepository extends JpaRepository<DeadLetterMessageEntity, String> { }

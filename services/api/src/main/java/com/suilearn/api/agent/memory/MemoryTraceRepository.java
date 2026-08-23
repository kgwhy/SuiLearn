package com.suilearn.api.agent.memory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryTraceRepository extends JpaRepository<MemoryTraceEntity, String> {}

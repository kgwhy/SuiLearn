package com.suilearn.api.agent.memory;

import java.time.Duration;
import java.util.Optional;

public interface SessionMemoryStore {
    Optional<SessionMemory> read(String key, Duration slidingTtl);

    void write(String key, SessionMemory memory, Duration ttl);

    long deleteByPrefix(String learnerKeyPrefix);
}

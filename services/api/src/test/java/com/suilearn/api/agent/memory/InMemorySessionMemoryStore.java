package com.suilearn.api.agent.memory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class InMemorySessionMemoryStore implements SessionMemoryStore {
    private final Clock clock;
    private final Map<String, Stored> memories = new HashMap<>();

    InMemorySessionMemoryStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<SessionMemory> read(String key, Duration slidingTtl) {
        Stored stored = memories.get(key);
        if (stored == null) {
            return Optional.empty();
        }
        if (!stored.expiresAt().isAfter(clock.instant())) {
            memories.remove(key);
            return Optional.empty();
        }
        memories.put(key, new Stored(stored.memory(), clock.instant().plus(slidingTtl)));
        return Optional.of(stored.memory());
    }

    @Override
    public void write(String key, SessionMemory memory, Duration ttl) {
        memories.put(key, new Stored(memory, clock.instant().plus(ttl)));
    }

    @Override
    public long deleteByPrefix(String learnerKeyPrefix) {
        long before = memories.size();
        memories.keySet().removeIf(key -> key.startsWith(learnerKeyPrefix));
        return before - memories.size();
    }

    Set<String> keys() {
        return Set.copyOf(memories.keySet());
    }

    private record Stored(SessionMemory memory, Instant expiresAt) {
    }
}

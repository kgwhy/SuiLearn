package com.suilearn.api.agent.memory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SessionMemoryService {
    private static final java.util.regex.Pattern TRANSCRIPT_SHAPE = java.util.regex.Pattern.compile(
        "(?i)^(user|assistant|system)\\s*:");
    private final SessionMemoryStore store;
    private final SessionMemoryKeyFactory keys;
    private final Duration ttl;
    private final int maximumTurns;

    public SessionMemoryService(SessionMemoryStore store, SessionMemoryKeyFactory keys, Duration ttl, int maximumTurns) {
        if (store == null || keys == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("session memory dependencies and positive ttl are required");
        }
        if (maximumTurns < 1) {
            throw new IllegalArgumentException("maximumTurns must be positive");
        }
        this.store = store;
        this.keys = keys;
        this.ttl = ttl;
        this.maximumTurns = maximumTurns;
    }

    public Optional<SessionMemory> read(String learnerId, String sessionId) {
        return store.read(keys.key(learnerId, sessionId), ttl);
    }

    public void append(String learnerId, String sessionId, SessionTurn turn) {
        if (turn == null || TRANSCRIPT_SHAPE.matcher(turn.summary().strip()).find()) {
            throw new IllegalArgumentException("session memory accepts summaries, not transcript-shaped content");
        }
        String key = keys.key(learnerId, sessionId);
        List<SessionTurn> turns = new ArrayList<>(store.read(key, ttl).orElse(new SessionMemory(List.of())).turns());
        turns.add(turn);
        if (turns.size() > maximumTurns) {
            turns = new ArrayList<>(turns.subList(turns.size() - maximumTurns, turns.size()));
        }
        store.write(key, new SessionMemory(turns), ttl);
    }

    public long deleteLearner(String learnerId) {
        return store.deleteByPrefix(keys.learnerPrefix(learnerId));
    }
}

package com.suilearn.api.agent.infrastructure.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.memory.SessionMemory;
import com.suilearn.api.agent.memory.SessionMemoryStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "suilearn.agent", name = "enabled", havingValue = "true")
public class RedisSessionMemoryStore implements SessionMemoryStore {
    private static final int SCAN_BATCH_SIZE = 100;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSessionMemoryStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SessionMemory> read(String key, Duration slidingTtl) {
        validateKey(key);
        requirePositive(slidingTtl);
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        Boolean refreshed = redis.expire(key, slidingTtl);
        if (!Boolean.TRUE.equals(refreshed)) {
            throw new IllegalStateException("failed to refresh session memory ttl");
        }
        return Optional.of(read(json));
    }

    @Override
    public void write(String key, SessionMemory memory, Duration ttl) {
        validateKey(key);
        requirePositive(ttl);
        if (memory == null) {
            throw new IllegalArgumentException("session memory is required");
        }
        redis.opsForValue().set(key, write(memory), ttl);
    }

    @Override
    public long deleteByPrefix(String learnerKeyPrefix) {
        validatePrefix(learnerKeyPrefix);
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(learnerKeyPrefix + "*").count(SCAN_BATCH_SIZE).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (keys.isEmpty()) {
            return 0;
        }
        Long deleted = redis.delete(keys);
        if (deleted == null) {
            throw new IllegalStateException("Redis did not report a deletion count");
        }
        return deleted;
    }

    private SessionMemory read(String json) {
        try {
            return objectMapper.readValue(json, SessionMemory.class);
        } catch (Exception failure) {
            throw new IllegalStateException("failed to deserialize session memory", failure);
        }
    }

    private String write(SessionMemory memory) {
        try {
            return objectMapper.writeValueAsString(memory);
        } catch (Exception failure) {
            throw new IllegalStateException("failed to serialize session memory", failure);
        }
    }

    private static void validateKey(String key) {
        if (key == null || !key.matches("suilearn:agent:session:v1:[0-9a-f]{32}:[0-9a-f]{32}")) {
            throw new IllegalArgumentException("uncontrolled session memory key");
        }
    }

    private static void validatePrefix(String prefix) {
        if (prefix == null || !prefix.matches("suilearn:agent:session:v1:[0-9a-f]{32}:")) {
            throw new IllegalArgumentException("uncontrolled session memory learner prefix");
        }
    }

    private static void requirePositive(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("positive ttl is required");
        }
    }
}

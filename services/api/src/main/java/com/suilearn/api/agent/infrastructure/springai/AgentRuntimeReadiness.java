package com.suilearn.api.agent.infrastructure.springai;

import org.springframework.data.redis.core.StringRedisTemplate;

public final class AgentRuntimeReadiness {
    private final StringRedisTemplate redis;

    public AgentRuntimeReadiness(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public static AgentRuntimeReadiness noOp() {
        return new AgentRuntimeReadiness(null);
    }

    public void requireAvailable() {
        if (redis == null) {
            return;
        }
        try {
            String pong = redis.execute((org.springframework.data.redis.core.RedisCallback<String>)
                connection -> connection.ping());
            if (pong == null || !pong.equalsIgnoreCase("PONG")) {
                throw new IllegalStateException("AGENT_SESSION_MEMORY_UNAVAILABLE");
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AGENT_SESSION_MEMORY_UNAVAILABLE");
        }
    }
}

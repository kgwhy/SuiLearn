package com.suilearn.api.agent.infrastructure.springai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

class AgentRuntimeReadinessTest {
    @Test
    void acceptsPongAndKeepsNoopAvailableForStoreFakes() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisCallback.class))).thenReturn("PONG");

        assertThatCode(() -> new AgentRuntimeReadiness(redis).requireAvailable()).doesNotThrowAnyException();
        assertThatCode(() -> AgentRuntimeReadiness.noOp().requireAvailable()).doesNotThrowAnyException();
    }

    @Test
    void mapsRedisFailureToStableSessionUnavailableCode() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisCallback.class))).thenThrow(new IllegalStateException("secret redis detail"));

        assertThatThrownBy(() -> new AgentRuntimeReadiness(redis).requireAvailable())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("AGENT_SESSION_MEMORY_UNAVAILABLE")
            .hasMessageNotContaining("secret");
    }
}

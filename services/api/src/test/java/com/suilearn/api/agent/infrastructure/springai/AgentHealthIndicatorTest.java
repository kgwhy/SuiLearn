package com.suilearn.api.agent.infrastructure.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.suilearn.api.agent.memory.SemanticMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.actuate.health.Status;

class AgentHealthIndicatorTest {
    @Test
    void reportsUpOnlyWhenModelRedisAndSemanticDependenciesAreAvailable() {
        var up = new AgentHealthIndicator(mock(ChatModel.class), AgentRuntimeReadiness.noOp(),
            mock(SemanticMemoryStore.class), null).health();
        var noModel = new AgentHealthIndicator(null, AgentRuntimeReadiness.noOp(),
            mock(SemanticMemoryStore.class), null).health();
        var noSemantic = new AgentHealthIndicator(mock(ChatModel.class), AgentRuntimeReadiness.noOp(),
            null, null).health();

        assertThat(up.getStatus()).isEqualTo(Status.UP);
        assertThat(noModel.getStatus()).isEqualTo(Status.DOWN);
        assertThat(noModel.getDetails()).containsEntry("code", "AGENT_MODEL_UNAVAILABLE");
        assertThat(noSemantic.getDetails()).containsEntry("code", "AGENT_SEMANTIC_MEMORY_UNAVAILABLE");
    }
}

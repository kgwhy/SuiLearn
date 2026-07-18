package com.suilearn.api.agent.infrastructure.springai;

import com.suilearn.api.agent.memory.SemanticMemoryStore;
import javax.sql.DataSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public final class AgentHealthIndicator implements HealthIndicator {
    private final ChatModel model;
    private final AgentRuntimeReadiness readiness;
    private final SemanticMemoryStore semanticStore;
    private final DataSource dataSource;

    AgentHealthIndicator(ChatModel model, AgentRuntimeReadiness readiness,
                         SemanticMemoryStore semanticStore, DataSource dataSource) {
        this.model = model;
        this.readiness = readiness;
        this.semanticStore = semanticStore;
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        if (model == null) return down("AGENT_MODEL_UNAVAILABLE");
        if (semanticStore == null) return down("AGENT_SEMANTIC_MEMORY_UNAVAILABLE");
        try {
            readiness.requireAvailable();
            if (dataSource != null) {
                try (var connection = dataSource.getConnection()) {
                    if (!connection.isValid(1)) return down("AGENT_SEMANTIC_MEMORY_UNAVAILABLE");
                }
            }
            return Health.up().withDetail("component", "study-agent").build();
        } catch (Exception exception) {
            return down(exception.getMessage() != null && exception.getMessage().startsWith("AGENT_")
                ? exception.getMessage() : "AGENT_DEPENDENCY_UNAVAILABLE");
        }
    }

    private Health down(String code) {
        return Health.down().withDetail("code", code).build();
    }
}

package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.config.AgentConfigurationProperties;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class AgentConfigurationPropertiesTest {
    @Test
    void bindsTypedConfigurationAtDocumentedDefaults() {
        AgentConfigurationProperties properties = bind(validProperties());

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.maxSteps()).isEqualTo(4);
        assertThat(properties.subagentMaxSteps()).isEqualTo(3);
        assertThat(properties.maxToolCalls()).isEqualTo(8);
        assertThat(properties.runTimeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(properties.contextMaxTokens()).isEqualTo(12000);
        assertThat(properties.practiceDefaultCount()).isEqualTo(3);
        assertThat(properties.session().ttl()).isEqualTo(Duration.ofHours(24));
        assertThat(properties.session().maxTurns()).isEqualTo(20);
        assertThat(properties.memory().topK()).isEqualTo(5);
        assertThat(properties.memory().minConfidence()).isEqualTo(0.80d);
    }

    @Test
    void rejectsEveryOutOfRangeBoundaryDuringBinding() {
        assertInvalid("suilearn.agent.max-steps", "9");
        assertInvalid("suilearn.agent.subagent-max-steps", "0");
        assertInvalid("suilearn.agent.max-tool-calls", "17");
        assertInvalid("suilearn.agent.run-timeout", "9s");
        assertInvalid("suilearn.agent.context-max-tokens", "2047");
        assertInvalid("suilearn.agent.practice-default-count", "6");
        assertInvalid("suilearn.agent.session.ttl", "169h");
        assertInvalid("suilearn.agent.session.max-turns", "51");
        assertInvalid("suilearn.agent.memory.top-k", "11");
        assertInvalid("suilearn.agent.memory.min-confidence", "0.49");
    }

    private void assertInvalid(String property, String value) {
        var properties = new java.util.HashMap<>(validProperties());
        properties.put(property, value);
        assertThatThrownBy(() -> bind(properties))
            .hasMessageNotContaining(value + " secret-body");
    }

    private AgentConfigurationProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
            .bind("suilearn.agent", Bindable.of(AgentConfigurationProperties.class))
            .orElseThrow(() -> new IllegalStateException("agent configuration was not bound"));
    }

    private Map<String, String> validProperties() {
        return Map.ofEntries(
            Map.entry("suilearn.agent.enabled", "false"),
            Map.entry("suilearn.agent.max-steps", "4"),
            Map.entry("suilearn.agent.subagent-max-steps", "3"),
            Map.entry("suilearn.agent.max-tool-calls", "8"),
            Map.entry("suilearn.agent.run-timeout", "90s"),
            Map.entry("suilearn.agent.context-max-tokens", "12000"),
            Map.entry("suilearn.agent.practice-default-count", "3"),
            Map.entry("suilearn.agent.session.ttl", "24h"),
            Map.entry("suilearn.agent.session.max-turns", "20"),
            Map.entry("suilearn.agent.memory.top-k", "5"),
            Map.entry("suilearn.agent.memory.min-confidence", "0.80"));
    }
}

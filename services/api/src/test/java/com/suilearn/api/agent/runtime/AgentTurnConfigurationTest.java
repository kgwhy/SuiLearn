package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentTurnConfigurationTest {
    @Test
    void websocketPropertyDefaultsTrueAndCanBeDisabled() {
        var properties = new AgentWebSocketProperties();
        assertThat(properties.isEnabled()).isTrue();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void applicationPropertiesAndPomUseServletWebSocketStack() throws Exception {
        String props = Files.readString(Path.of("src", "main", "resources", "application.properties"));
        assertThat(props).contains("suilearn.agent.websocket.enabled=${SUILEARN_AGENT_WEBSOCKET_ENABLED:true}");
        String localExample = Files.readString(Path.of("config", "local.properties.example"));
        assertThat(localExample).contains("suilearn.agent.websocket.enabled=${SUILEARN_AGENT_WEBSOCKET_ENABLED:true}");
        String envExample = Files.readString(Path.of("..", "..", ".env.example"));
        assertThat(envExample).contains("SUILEARN_AGENT_WEBSOCKET_ENABLED=true");
        String compose = Files.readString(Path.of("..", "..", "compose.yml"));
        assertThat(compose).contains("SUILEARN_AGENT_WEBSOCKET_ENABLED: ${SUILEARN_AGENT_WEBSOCKET_ENABLED:-true}");
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains("spring-boot-starter-websocket");
        assertThat(pom).doesNotContain("spring-boot-starter-webflux", "reactor-netty");
    }
}

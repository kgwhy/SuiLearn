package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class HealthLayeringConfigurationTest {
    @Test
    void keepsHttpReadinessSeparateFromBackgroundProcessingDependencies() throws IOException {
        var properties = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        }

        assertThat(properties.getProperty("management.endpoint.health.group.liveness.include")).isEqualTo("livenessState");
        assertThat(properties.getProperty("management.endpoint.health.group.readiness.include")).isEqualTo("readinessState,db");
        assertThat(properties.getProperty("management.endpoint.health.group.processing.include")).isEqualTo("rabbit,minio");
        assertThat(properties.getProperty("management.health.rabbit.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("suilearn.rabbitmq.health-timeout-ms"))
            .isEqualTo("${SUILEARN_RABBITMQ_HEALTH_TIMEOUT_MS:1000}");
    }
}

package com.suilearn.api.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

final class RabbitProcessingHealthProbe implements HealthIndicator {
    private static final String PROBE_QUEUE = "document.processing";
    private final RabbitTemplate rabbitTemplate;

    RabbitProcessingHealthProbe(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Health health() {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive(PROBE_QUEUE);
                return null;
            });
            return Health.up().build();
        } catch (Exception unavailable) {
            return Health.down().withDetail("reason", "unavailable").build();
        }
    }
}

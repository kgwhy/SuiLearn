package com.suilearn.api.config;

import java.time.Duration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProcessingHealthConfiguration {
    @Bean(name = "rabbitHealthIndicator")
    HealthIndicator boundedRabbitHealthIndicator(
        RabbitTemplate rabbitTemplate,
        @Value("${suilearn.rabbitmq.health-timeout-ms:1000}") long timeoutMillis
    ) {
        return new BoundedHealthIndicator(new RabbitProcessingHealthProbe(rabbitTemplate), Duration.ofMillis(timeoutMillis));
    }
}

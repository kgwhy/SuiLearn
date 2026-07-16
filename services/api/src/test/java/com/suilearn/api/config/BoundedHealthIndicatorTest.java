package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

class BoundedHealthIndicatorTest {
    @Test
    void returnsDownWithinTheConfiguredBoundWhenTheRabbitProbeBlocks() throws InterruptedException {
        var started = new CountDownLatch(1);
        HealthIndicator blockingProbe = () -> {
            started.countDown();
            try {
                Thread.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return Health.up().build();
        };
        var indicator = new BoundedHealthIndicator(blockingProbe, Duration.ofMillis(50));

        long startedAt = System.nanoTime();
        var health = indicator.health();
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsEntry("reason", "timeout");
        assertThat(elapsedMillis).isLessThan(500);
        indicator.close();
    }
}

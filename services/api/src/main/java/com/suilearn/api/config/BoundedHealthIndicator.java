package com.suilearn.api.config;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

final class BoundedHealthIndicator implements HealthIndicator, AutoCloseable {
    private final HealthIndicator delegate;
    private final Duration timeout;
    private final ExecutorService executor;

    BoundedHealthIndicator(HealthIndicator delegate, Duration timeout) {
        this(delegate, timeout, Executors.newVirtualThreadPerTaskExecutor());
    }

    BoundedHealthIndicator(HealthIndicator delegate, Duration timeout, ExecutorService executor) {
        this.delegate = delegate;
        this.timeout = timeout;
        this.executor = executor;
    }

    @Override
    public Health health() {
        Future<Health> probe = executor.submit(delegate::health);
        try {
            return probe.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            probe.cancel(true);
            return Health.down().withDetail("reason", "timeout").build();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail("reason", "interrupted").build();
        } catch (ExecutionException failedProbe) {
            return Health.down().withDetail("reason", "unavailable").build();
        }
    }

    @PreDestroy
    @Override
    public void close() {
        executor.shutdownNow();
    }
}

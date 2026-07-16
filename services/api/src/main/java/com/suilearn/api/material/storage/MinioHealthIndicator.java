package com.suilearn.api.material.storage;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public final class MinioHealthIndicator implements HealthIndicator {
    private final MinioObjectGateway gateway;
    private final String bucket;

    public MinioHealthIndicator(MinioObjectGateway gateway, String bucket) {
        this.gateway = gateway;
        this.bucket = bucket;
    }

    @Override
    public Health health() {
        try {
            return gateway.bucketExists(bucket) ? Health.up().build() : Health.down().withDetail("bucket", "missing").build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("storage", "unavailable").build();
        }
    }
}

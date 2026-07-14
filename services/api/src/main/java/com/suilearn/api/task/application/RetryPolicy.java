package com.suilearn.api.task.application;

public final class RetryPolicy {
    private final int maxAttempts;

    public RetryPolicy(int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        this.maxAttempts = maxAttempts;
    }

    DeliveryDecision next(int attempt, FailureKind failureKind) {
        return failureKind == FailureKind.PERMANENT || attempt >= maxAttempts
            ? DeliveryDecision.DEAD_LETTER : DeliveryDecision.RETRY;
    }
}

package com.suilearn.api.ai;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

public final class AiFailureClassifier {
    private AiFailureClassifier() { }

    public static AiFailureKind classify(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof CallNotPermittedException) return AiFailureKind.CIRCUIT_OPEN;
            if (current instanceof HttpTimeoutException || current instanceof TimeoutException) return AiFailureKind.TIMEOUT;
            if (current instanceof AiHttpStatusException status) return classifyHttpStatus(status.statusCode());
        }
        return AiFailureKind.TRANSIENT;
    }

    public static AiFailureKind classifyHttpStatus(int statusCode) {
        if (statusCode == 408 || statusCode == 504) return AiFailureKind.TIMEOUT;
        if (statusCode == 429) return AiFailureKind.RATE_LIMITED;
        return statusCode >= 500 ? AiFailureKind.TRANSIENT : AiFailureKind.PERMANENT;
    }

    public static boolean countsTowardCircuitBreaker(Throwable failure) {
        return switch (classify(failure)) {
            case TIMEOUT, RATE_LIMITED, TRANSIENT -> true;
            case SUCCESS, PERMANENT, CIRCUIT_OPEN -> false;
        };
    }
}

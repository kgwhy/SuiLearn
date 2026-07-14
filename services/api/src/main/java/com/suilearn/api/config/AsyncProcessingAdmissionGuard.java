package com.suilearn.api.config;

/** Rejects new imports when the durable processing pipeline is intentionally unavailable. */
public final class AsyncProcessingAdmissionGuard {
    public static final String DISABLED_MESSAGE =
        "ASYNC_PROCESSING_DISABLED: new material uploads and imports are unavailable";

    private final boolean enabled;

    public AsyncProcessingAdmissionGuard(boolean enabled) {
        this.enabled = enabled;
    }

    public void requireNewImportAdmission() {
        if (!enabled) {
            throw new AsyncProcessingDisabledException();
        }
    }

    public static final class AsyncProcessingDisabledException extends IllegalStateException {
        public AsyncProcessingDisabledException() {
            super(DISABLED_MESSAGE);
        }
    }
}

package com.suilearn.api.material.application;

/** Raised only when a migrated text-only material has no original binary asset. */
public final class MaterialOriginalUnavailableException extends RuntimeException {
    public MaterialOriginalUnavailableException() {
        super("The original file is unavailable for this legacy material.");
    }
}

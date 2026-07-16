package com.suilearn.api.runtimefixture;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Mutable, in-memory fault state available only under the explicit runtime-fixture profile. */
@Component
@Profile("runtime-fixture")
public final class RuntimeFixtureControl {
    public enum Mode { NORMAL, TIMEOUT }

    private volatile Mode ocrMode = Mode.NORMAL;
    private volatile Mode aiMode = Mode.NORMAL;

    public Mode ocrMode() { return ocrMode; }

    public Mode aiMode() { return aiMode; }

    public void setOcrMode(Mode mode) { this.ocrMode = mode; }

    public void setAiMode(Mode mode) { this.aiMode = mode; }

    public void reset() {
        ocrMode = Mode.NORMAL;
        aiMode = Mode.NORMAL;
    }
}

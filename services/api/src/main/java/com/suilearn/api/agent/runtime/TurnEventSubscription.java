package com.suilearn.api.agent.runtime;

import java.util.Objects;

public final class TurnEventSubscription implements AutoCloseable {
    private final TurnEventBus bus;
    private final TurnEventListener listener;
    private volatile boolean closed;

    TurnEventSubscription(TurnEventBus bus, TurnEventListener listener) {
        this.bus = Objects.requireNonNull(bus, "bus");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            bus.removeListener(listener);
        }
    }
}

package com.suilearn.api.agent.runtime;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.function.LongFunction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-turn live event channel. Persisted events are offered to a bounded queue and
 * drained by one virtual-thread dispatcher. A slow listener can block only this
 * turn's dispatcher; the queue stays bounded and clients can always resume with
 * {@code afterSeq} from PostgreSQL.
 */
public final class TurnEventBus {
    private static final Logger LOG = LoggerFactory.getLogger(TurnEventBus.class);

    public static final int DEFAULT_QUEUE_CAPACITY = 256;

    private final String turnId;
    private final String sessionId;
    private final ArrayBlockingQueue<StreamEvent> queue;
    private final CopyOnWriteArraySet<TurnEventListener> listeners = new CopyOnWriteArraySet<>();
    private final CompletableFuture<StreamEvent> terminal = new CompletableFuture<>();
    private final Object deliveryLock = new Object();
    private volatile StreamEvent terminalEvent;
    private volatile boolean closed;
    private final Thread dispatcher;

    public TurnEventBus(String turnId, String sessionId) {
        this(turnId, sessionId, DEFAULT_QUEUE_CAPACITY);
    }

    public TurnEventBus(String turnId, String sessionId, int queueCapacity) {
        this.turnId = requireText(turnId, "turnId");
        this.sessionId = requireText(sessionId, "sessionId");
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be >= 1");
        }
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.dispatcher = Thread.ofVirtual().name("suilearn-turn-bus-" + turnId).start(this::drain);
    }

    public String turnId() {
        return turnId;
    }

    public String sessionId() {
        return sessionId;
    }

    public boolean isTerminal() {
        return terminalEvent != null;
    }

    public StreamEvent terminalEvent() {
        return terminalEvent;
    }

    /**
     * Puts an already persisted event on the live channel.
     *
     * @return false when this bus already reached its unique terminal state.
     */
    public boolean publishPersisted(StreamEvent event) {
        Objects.requireNonNull(event, "event");
        if (!turnId.equals(event.turnId()) || !sessionId.equals(event.sessionId())) {
            throw new IllegalArgumentException("event does not belong to this turn bus");
        }
        if (terminalEvent != null) {
            return false;
        }
        if (event.type().isTerminal()) {
            markTerminal(event);
        }
        if (!queue.offer(event)) {
            LOG.warn("Turn live-event queue full; dropping live frame for turnId={} seq={}; clients can resume_from",
                turnId, event.seq());
        }
        return true;
    }

    public CompletableFuture<StreamEvent> terminalFuture() {
        return terminal;
    }

    public StreamEvent awaitTerminal(Duration timeout) throws InterruptedException, TimeoutException {
        try {
            return terminal.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("terminal future failed", exception.getCause());
        }
    }

    /**
     * Marks this bus terminal without enqueueing a live frame. Used when a bus is
     * recreated for an already terminal turn: replay is served from the store only.
     */
    public boolean markTerminal(StreamEvent event) {
        Objects.requireNonNull(event, "event");
        if (!turnId.equals(event.turnId()) || !event.type().isTerminal()) {
            throw new IllegalArgumentException("terminal event must belong to this turn bus");
        }
        if (terminalEvent != null) {
            return false;
        }
        terminalEvent = event;
        terminal.complete(event);
        return true;
    }

    public TurnEventSubscription subscribe(TurnEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return new TurnEventSubscription(this, listener);
    }

    /**
     * Replays persisted events and then attaches the listener under the same delivery
     * lock used by the drain loop, so no event can be delivered in the gap between
     * replay and subscription.
     */
    public TurnEventSubscription subscribeReplaying(
        TurnEventListener listener,
        long afterSeq,
        LongFunction<java.util.List<StreamEvent>> replaySource
    ) {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(replaySource, "replaySource");
        synchronized (deliveryLock) {
            for (StreamEvent event : replaySource.apply(afterSeq)) {
                listener.onEvent(event);
            }
            listeners.add(listener);
        }
        return new TurnEventSubscription(this, listener);
    }

    public void removeListener(TurnEventListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        closed = true;
        dispatcher.interrupt();
        if (!terminal.isDone()) {
            terminal.cancel(false);
        }
    }

    private void drain() {
        while (!closed) {
            StreamEvent event;
            try {
                event = queue.take();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (deliveryLock) {
                for (TurnEventListener listener : listeners) {
                    try {
                        listener.onEvent(event);
                    } catch (RuntimeException exception) {
                        LOG.warn("Turn live-event listener failed for turnId={} seq={}; removing listener",
                            turnId, event.seq(), exception);
                        listeners.remove(listener);
                    }
                }
            }
            if (event.type().isTerminal()) {
                closed = true;
                return;
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}

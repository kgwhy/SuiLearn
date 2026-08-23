package com.suilearn.api.agent.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serialized per-turn event publisher. It allocates seq, persists first and only
 * then offers the event to the live bus.
 */
public final class TurnEventSink {
    public static final int MAX_EVENT_PAYLOAD_BYTES = 64 * 1024;

    private final String turnId;
    private final String sessionId;
    private final TurnStore store;
    private final TurnEventBus bus;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AtomicLong nextSeq;

    public TurnEventSink(String turnId, String sessionId, long lastSeq, TurnStore store,
                         TurnEventBus bus, ObjectMapper objectMapper, Clock clock) {
        this.turnId = requireText(turnId, "turnId");
        this.sessionId = requireText(sessionId, "sessionId");
        if (lastSeq < 1) {
            throw new IllegalArgumentException("lastSeq must be >= 1");
        }
        this.store = store;
        this.bus = bus;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.nextSeq = new AtomicLong(lastSeq + 1);
    }

    public TurnEventBus bus() {
        return bus;
    }

    public synchronized StreamEvent publish(EventType type, String source, String stage,
                                            String content, Map<String, Object> metadata) {
        if (bus.isTerminal()) {
            return null;
        }
        long seq = nextSeq.get();
        var event = event(type, source, stage, content, metadata, seq);
        assertPayloadWithinLimit(event);
        store.appendEvent(event);
        nextSeq.incrementAndGet();
        bus.publishPersisted(event);
        return event;
    }

    /**
     * Publishes an already allocated terminal event with an explicit turn status.
     * Used by normal terminal transitions and orphan recovery.
     */
    public synchronized StreamEvent publishTerminal(EventType type, TurnStatus status, String source,
                                                    String stage, String content,
                                                    Map<String, Object> metadata) {
        if (!type.isTerminal()) {
            throw new IllegalArgumentException("terminal event type required");
        }
        if (bus.isTerminal()) {
            return bus.terminalEvent();
        }
        long seq = nextSeq.get();
        var event = event(type, source, stage, content, metadata, seq);
        assertPayloadWithinLimit(event);
        store.appendTerminalEvent(event, status);
        nextSeq.incrementAndGet();
        bus.publishPersisted(event);
        return event;
    }

    private StreamEvent event(EventType type, String source, String stage, String content,
                              Map<String, Object> metadata, long seq) {
        return new StreamEvent(turnId, sessionId, seq, type, source, stage,
            content == null ? "" : content, metadata, clock.instant());
    }

    private void assertPayloadWithinLimit(StreamEvent event) {
        try {
            int bytes = objectMapper.writeValueAsBytes(event).length;
            if (bytes > MAX_EVENT_PAYLOAD_BYTES) {
                throw new TurnEventPayloadException();
            }
        } catch (JsonProcessingException exception) {
            throw new TurnApiException(TurnErrorCode.INVALID_EVENT_PAYLOAD);
        }
    }

    public Instant now() {
        return clock.instant();
    }

    public static TurnEventSink create(String turnId, String sessionId, long lastSeq, TurnStore store,
                                       TurnEventBus bus, ObjectMapper objectMapper) {
        return new TurnEventSink(turnId, sessionId, lastSeq, store, bus, objectMapper, Clock.systemUTC());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}

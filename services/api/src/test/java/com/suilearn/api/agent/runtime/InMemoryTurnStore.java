package com.suilearn.api.agent.runtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryTurnStore implements TurnStore {
    private final Map<String, TurnRecord> turns = new LinkedHashMap<>();
    private final Map<String, List<StreamEvent>> events = new LinkedHashMap<>();

    @Override
    public synchronized TurnRecord createTurn(TurnContext context, String inputMessageId, StreamEvent firstEvent) {
        if (firstEvent.seq() != 1 || firstEvent.type() != EventType.TURN_STARTED
            || !firstEvent.turnId().equals(context.turnId())) {
            throw new TurnStoreException("first event must be seq=1 turn_started for the new turn");
        }
        var record = new TurnRecord(context.turnId(), context.sessionId(), context.learnerId(), context.capability(),
            TurnStatus.RUNNING, context.scope(), context.sources(), inputMessageId, 1, firstEvent.ts(),
            firstEvent.ts(), null);
        turns.put(context.turnId(), record);
        events.computeIfAbsent(context.turnId(), ignored -> new ArrayList<>()).add(firstEvent);
        return record;
    }

    @Override
    public synchronized Optional<TurnRecord> findTurn(String turnId) {
        return Optional.ofNullable(turns.get(turnId));
    }

    @Override
    public synchronized Optional<TurnRecord> findActiveTurn(String sessionId) {
        return turns.values().stream()
            .filter(turn -> turn.sessionId().equals(sessionId) && turn.status().isActive())
            .max(Comparator.comparing(TurnRecord::createdAt));
    }

    @Override
    public synchronized List<StreamEvent> findEventsAfter(String turnId, long afterSeq) {
        return events.getOrDefault(turnId, List.of()).stream()
            .filter(event -> event.seq() > afterSeq)
            .sorted(Comparator.comparingLong(StreamEvent::seq))
            .toList();
    }

    @Override
    public synchronized Optional<StreamEvent> findLastEvent(String turnId) {
        var list = events.get(turnId);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.getLast());
    }

    @Override
    public synchronized TurnRecord appendEvent(StreamEvent event) {
        var record = requireTurn(event.turnId());
        requireNextSeq(record, event.seq());
        if (events.get(event.turnId()).stream().anyMatch(existing -> existing.seq() == event.seq())) {
            throw new TurnEventConflictException(event.turnId(), event.seq());
        }
        events.get(event.turnId()).add(event);
        TurnStatus status = record.status();
        Instant finishedAt = record.finishedAt();
        if (event.type().isTerminal()) {
            status = terminalStatus(event.type());
            finishedAt = event.ts();
        }
        var updated = new TurnRecord(record.turnId(), record.sessionId(), record.learnerId(), record.capability(),
            status, record.scope(), record.sources(), record.inputMessageId(), event.seq(), record.createdAt(),
            record.startedAt(), finishedAt);
        turns.put(event.turnId(), updated);
        return updated;
    }

    @Override
    public synchronized TurnRecord appendTerminalEvent(StreamEvent event, TurnStatus terminalStatus) {
        if (!event.type().isTerminal() || !terminalStatus.isTerminal()) {
            throw new TurnStoreException("terminal event and terminal status are required");
        }
        var record = requireTurn(event.turnId());
        requireNextSeq(record, event.seq());
        events.get(event.turnId()).add(event);
        var updated = new TurnRecord(record.turnId(), record.sessionId(), record.learnerId(), record.capability(),
            terminalStatus, record.scope(), record.sources(), record.inputMessageId(), event.seq(),
            record.createdAt(), record.startedAt(), event.ts());
        turns.put(event.turnId(), updated);
        return updated;
    }

    @Override
    public synchronized TurnRecord updateStatus(String turnId, TurnStatus status) {
        var record = requireTurn(turnId);
        var updated = new TurnRecord(record.turnId(), record.sessionId(), record.learnerId(), record.capability(),
            status, record.scope(), record.sources(), record.inputMessageId(), record.lastSeq(),
            record.createdAt(), record.startedAt(), status.isTerminal() ? record.finishedAt() == null ? Instant.now() : record.finishedAt() : record.finishedAt());
        turns.put(turnId, updated);
        return updated;
    }

    @Override
    public synchronized List<TurnRecord> findOrphanedRunning() {
        return turns.values().stream().filter(turn -> turn.status() == TurnStatus.RUNNING).toList();
    }

    @Override
    public synchronized long countEvents(String turnId) {
        return events.getOrDefault(turnId, List.of()).size();
    }

    private TurnRecord requireTurn(String turnId) {
        var record = turns.get(turnId);
        if (record == null) {
            throw new TurnStoreException("turn does not exist: " + turnId);
        }
        return record;
    }

    private static void requireNextSeq(TurnRecord record, long seq) {
        if (seq != record.lastSeq() + 1) {
            throw new TurnStoreException("turn event seq gap: expected " + (record.lastSeq() + 1) + " but got " + seq);
        }
    }

    private static TurnStatus terminalStatus(EventType type) {
        return switch (type) {
            case DONE -> TurnStatus.COMPLETED;
            case CANCELLED -> TurnStatus.CANCELLED;
            case FAILED -> TurnStatus.FAILED;
            default -> throw new TurnStoreException("not a terminal event: " + type);
        };
    }
}

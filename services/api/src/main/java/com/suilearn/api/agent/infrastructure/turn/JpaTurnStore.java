package com.suilearn.api.agent.infrastructure.turn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.SourceSelection;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnContext;
import com.suilearn.api.agent.runtime.TurnEventConflictException;
import com.suilearn.api.agent.runtime.TurnRecord;
import com.suilearn.api.agent.runtime.TurnStatus;
import com.suilearn.api.agent.runtime.TurnStore;
import com.suilearn.api.agent.runtime.TurnStoreException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class JpaTurnStore implements TurnStore {
    private static final TypeReference<List<SourceSelection>> SOURCE_LIST = new TypeReference<>() {};

    private final TurnJpaRepository turns;
    private final TurnEventJpaRepository events;
    private final SessionMessageJpaRepository messages;
    private final ObjectMapper objectMapper;

    public JpaTurnStore(TurnJpaRepository turns, TurnEventJpaRepository events,
                        SessionMessageJpaRepository messages, ObjectMapper objectMapper) {
        this.turns = turns;
        this.events = events;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Override
    public TurnRecord createTurn(TurnContext context, String inputMessageId, StreamEvent firstEvent) {
        requireFirstEvent(context, firstEvent);
        var now = firstEvent.ts();
        var turn = new TurnEntity(context.turnId(), context.sessionId(), context.learnerId(), context.capability(),
            TurnStatus.RUNNING.name(), writeJson(context.scope()), writeJson(context.sources()), inputMessageId,
            1, now, now, null, now);
        messages.saveAndFlush(new SessionMessageEntity(inputMessageId, context.sessionId(), context.learnerId(),
            context.turnId(), "USER", context.userMessage(), now));
        turns.saveAndFlush(turn);
        appendEventEntity(firstEvent);
        return map(turn);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TurnRecord> findTurn(String turnId) {
        return turns.findById(turnId).map(this::map);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TurnRecord> findActiveTurn(String sessionId) {
        return turns.findFirstBySessionIdAndStatusInOrderByCreatedAtDesc(
            sessionId, List.of(TurnStatus.RUNNING.name(), TurnStatus.WAITING_INPUT.name())).map(this::map);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StreamEvent> findEventsAfter(String turnId, long afterSeq) {
        return events.findByIdTurnIdAndIdSeqGreaterThanOrderByIdSeqAsc(turnId, afterSeq).stream()
            .map(this::mapEvent)
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<StreamEvent> findLastEvent(String turnId) {
        return events.findFirstByIdTurnIdOrderByIdSeqDesc(turnId).map(this::mapEvent);
    }

    @Transactional
    @Override
    public TurnRecord appendEvent(StreamEvent event) {
        var turn = requireTurn(event.turnId());
        requireNextSeq(turn, event.seq());
        appendEventEntity(event);
        var now = event.ts();
        turn.markLastSeq(event.seq(), now);
        if (event.type().isTerminal()) {
            turn.markStatus(statusFor(event.type()).name(), now);
        }
        turns.saveAndFlush(turn);
        return map(turn);
    }

    @Transactional
    @Override
    public TurnRecord appendTerminalEvent(StreamEvent event, TurnStatus terminalStatus) {
        if (!event.type().isTerminal() || !terminalStatus.isTerminal()) {
            throw new TurnStoreException("terminal event and terminal status are required");
        }
        var turn = requireTurn(event.turnId());
        requireNextSeq(turn, event.seq());
        appendEventEntity(event);
        var now = event.ts();
        turn.markLastSeq(event.seq(), now);
        turn.markStatus(terminalStatus.name(), now);
        turns.saveAndFlush(turn);
        return map(turn);
    }

    @Transactional
    @Override
    public TurnRecord updateStatus(String turnId, TurnStatus status) {
        var turn = requireTurn(turnId);
        turn.markStatus(status.name(), Instant.now());
        turns.saveAndFlush(turn);
        return map(turn);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TurnRecord> findOrphanedRunning() {
        return turns.findByStatus(TurnStatus.RUNNING.name()).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public long countEvents(String turnId) {
        return events.countByIdTurnId(turnId);
    }

    private void appendEventEntity(StreamEvent event) {
        try {
            events.saveAndFlush(new TurnEventEntity(new TurnEventId(event.turnId(), event.seq()),
                event.sessionId(), event.type(), objectMapper.writeValueAsString(event), event.ts()));
        } catch (JsonProcessingException exception) {
            throw new TurnStoreException("failed to serialize turn event", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new TurnEventConflictException(event.turnId(), event.seq());
        }
    }

    private TurnRecord map(TurnEntity entity) {
        try {
            var scope = objectMapper.readValue(entity.getScopeJson(), StudyScope.class);
            var sources = entity.getSourceSelectionJson() == null || entity.getSourceSelectionJson().isBlank()
                ? List.<SourceSelection>of()
                : objectMapper.readValue(entity.getSourceSelectionJson(), SOURCE_LIST);
            return new TurnRecord(entity.getId(), entity.getSessionId(), entity.getLearnerId(), entity.getCapability(),
                TurnStatus.valueOf(entity.getStatus()), scope, sources, entity.getInputMessageId(), entity.getLastSeq(),
                entity.getCreatedAt(), entity.getStartedAt(), entity.getFinishedAt());
        } catch (JsonProcessingException exception) {
            throw new TurnStoreException("failed to read turn record", exception);
        }
    }

    private StreamEvent mapEvent(TurnEventEntity entity) {
        try {
            return objectMapper.readValue(entity.getPayload(), StreamEvent.class);
        } catch (JsonProcessingException exception) {
            throw new TurnStoreException("failed to read turn event payload", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new TurnStoreException("failed to serialize turn scope", exception);
        }
    }

    private TurnEntity requireTurn(String turnId) {
        return turns.findById(turnId)
            .orElseThrow(() -> new TurnStoreException("turn does not exist: " + turnId));
    }

    private static void requireFirstEvent(TurnContext context, StreamEvent firstEvent) {
        if (firstEvent.seq() != 1 || firstEvent.type() != EventType.TURN_STARTED
            || !firstEvent.turnId().equals(context.turnId())) {
            throw new TurnStoreException("first event must be seq=1 turn_started for the new turn");
        }
    }

    private static void requireNextSeq(TurnEntity turn, long seq) {
        if (seq != turn.getLastSeq() + 1) {
            throw new TurnStoreException("turn event seq gap: expected " + (turn.getLastSeq() + 1) + " but got " + seq);
        }
    }

    private static TurnStatus statusFor(EventType type) {
        return switch (type) {
            case DONE -> TurnStatus.COMPLETED;
            case CANCELLED -> TurnStatus.CANCELLED;
            case FAILED -> TurnStatus.FAILED;
            default -> throw new TurnStoreException("not a terminal event: " + type);
        };
    }
}

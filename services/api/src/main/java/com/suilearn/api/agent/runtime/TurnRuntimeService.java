package com.suilearn.api.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnRuntimeService {
    private static final Logger LOG = LoggerFactory.getLogger(TurnRuntimeService.class);

    private final TurnStore store;
    private final TurnExecutor executor;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Set<String> supportedCapabilities;
    private final ExecutorService worker;
    private final ConcurrentHashMap<String, TurnEventBus> buses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<TurnReply>> replies = new ConcurrentHashMap<>();

    public TurnRuntimeService(TurnStore store, TurnExecutor executor, ObjectMapper objectMapper,
                              Clock clock, Set<String> supportedCapabilities) {
        this(store, executor, objectMapper, clock, supportedCapabilities,
            Executors.newVirtualThreadPerTaskExecutor());
    }

    public TurnRuntimeService(TurnStore store, TurnExecutor executor, ObjectMapper objectMapper,
                              Clock clock, Set<String> supportedCapabilities, ExecutorService worker) {
        this.store = store;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.supportedCapabilities = Set.copyOf(supportedCapabilities);
        this.worker = worker;
    }

    public StartTurnOutcome start(StartTurnCommand command) {
        validateCapability(command.capability());
        store.findActiveTurn(command.sessionId()).ifPresent(active -> {
            throw new TurnApiException(TurnErrorCode.AGENT_TURN_ACTIVE_CONFLICT);
        });

        var now = clock.instant();
        var turnId = newId("turn_");
        var sessionId = command.sessionId() == null || command.sessionId().isBlank()
            ? newId("sess_") : command.sessionId().strip();
        var messageId = newId("msg_");
        var context = new TurnContext(turnId, sessionId, command.learnerId(), command.capability(),
            command.scope(), command.sources(), command.message(), List.of(), command.attachments(), Map.of());
        var firstEvent = new StreamEvent(turnId, sessionId, 1, EventType.TURN_STARTED,
            context.capability(), null, "", Map.of(), now);

        var record = store.createTurn(context, messageId, firstEvent);
        var bus = new TurnEventBus(turnId, sessionId);
        buses.put(turnId, bus);
        bus.terminalFuture().whenComplete((terminal, error) -> {
            buses.remove(turnId, bus);
            replies.remove(turnId);
        });
        replies.put(turnId, new LinkedBlockingQueue<>());
        var sink = new TurnEventSink(turnId, sessionId, record.lastSeq(), store, bus, objectMapper, clock,
            this::awaitReply);
        worker.submit(() -> executeSafely(context, sink));
        return new StartTurnOutcome(record, bus);
    }

    public TurnResult awaitResult(String turnId, Duration timeout) throws InterruptedException, TimeoutException {
        var bus = requireBus(turnId);
        var terminal = bus.awaitTerminal(timeout);
        var record = store.findTurn(turnId).orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
        return new TurnResult(record.turnId(), record.sessionId(), record.status(), record.lastSeq(), terminal,
            record.createdAt(), record.finishedAt() == null ? terminal.ts() : record.finishedAt());
    }

    public TurnEventPage eventsAfter(String turnId, long afterSeq) {
        if (afterSeq < 0) {
            throw new TurnApiException(TurnErrorCode.INVALID_AGENT_REQUEST);
        }
        var record = store.findTurn(turnId).orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
        return new TurnEventPage(turnId, afterSeq, record.lastSeq(), store.findEventsAfter(turnId, afterSeq));
    }

    public TurnEventSubscription subscribeReplaying(String turnId, long afterSeq, TurnEventListener listener) {
        if (afterSeq < 0) {
            throw new TurnApiException(TurnErrorCode.INVALID_AGENT_REQUEST);
        }
        var bus = requireBus(turnId);
        return bus.subscribeReplaying(listener, afterSeq, seq -> store.findEventsAfter(turnId, seq));
    }

    public TurnRecord cancel(String turnId) {
        var record = store.findTurn(turnId).orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
        if (record.status().isTerminal()) {
            throw new TurnApiException(TurnErrorCode.AGENT_TURN_TERMINAL);
        }
        var sink = sinkFor(turnId, record.sessionId(), record.lastSeq());
        sink.publishTerminal(EventType.CANCELLED, TurnStatus.CANCELLED, "turn-runtime", "cancel",
            "Turn cancelled by client.", Map.of("code", "TURN_CANCELLED"));
        return store.findTurn(turnId).orElse(record);
    }

    public TurnRecord submitReply(String turnId, String text, Map<String, Object> answers) {
        var record = store.findTurn(turnId).orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
        if (record.status() != TurnStatus.WAITING_INPUT) {
            throw new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_WAITING_FOR_INPUT);
        }
        if ((text == null || text.isBlank()) && (answers == null || answers.isEmpty())) {
            throw new TurnApiException(TurnErrorCode.INVALID_AGENT_REQUEST);
        }
        BlockingQueue<TurnReply> queue = replies.get(turnId);
        if (queue == null) {
            throw new TurnApiException(TurnErrorCode.AGENT_DEPENDENCY_UNAVAILABLE);
        }
        store.updateStatus(turnId, TurnStatus.RUNNING);
        if (!queue.offer(new TurnReply(text, answers))) {
            throw new TurnApiException(TurnErrorCode.AGENT_DEPENDENCY_UNAVAILABLE);
        }
        return store.findTurn(turnId).orElse(record);
    }

    public ActiveTurnInfo checkActiveTurn(String sessionId) {
        var active = store.findActiveTurn(sessionId);
        if (active.isEmpty()) {
            return ActiveTurnInfo.none(sessionId);
        }
        var record = active.orElseThrow();
        var bus = buses.get(record.turnId());
        if (bus != null && !bus.isTerminal()) {
            return new ActiveTurnInfo(sessionId, record.turnId(), record.status());
        }
        if (record.status() == TurnStatus.RUNNING) {
            var orphaned = recoverOrphan(record);
            return new ActiveTurnInfo(sessionId, orphaned.turnId(), orphaned.status());
        }
        return new ActiveTurnInfo(sessionId, record.turnId(), record.status());
    }

    public List<TurnRecord> recoverOrphans() {
        return store.findOrphanedRunning().stream().map(running -> {
            try {
                return recoverOrphan(running);
            } catch (RuntimeException exception) {
                LOG.error("Failed to recover orphaned turn {}", running.turnId(), exception);
                return running;
            }
        }).toList();
    }

    public Optional<TurnEventBus> bus(String turnId) {
        return Optional.ofNullable(buses.get(turnId));
    }

    private TurnReply awaitReply(String turnId, Duration timeout) throws InterruptedException, TimeoutException {
        BlockingQueue<TurnReply> queue = replies.get(turnId);
        if (queue == null) {
            throw new TurnApiException(TurnErrorCode.AGENT_DEPENDENCY_UNAVAILABLE);
        }
        TurnReply reply = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (reply == null) {
            throw new TimeoutException("turn reply timed out: " + turnId);
        }
        return reply;
    }

    @PreDestroy
    public void close() {
        buses.values().forEach(TurnEventBus::close);
        buses.clear();
        replies.clear();
        worker.shutdownNow();
    }

    private void executeSafely(TurnContext context, TurnEventSink sink) {
        try {
            executor.execute(context, sink);
        } catch (RuntimeException exception) {
            LOG.warn("Turn executor failed for turnId={}", context.turnId(), exception);
            try {
                sink.publishTerminal(EventType.FAILED, TurnStatus.FAILED, "turn-runtime", "executor-failure",
                    "Turn executor failed.", Map.of("code", "AGENT_DEPENDENCY_UNAVAILABLE"));
            } catch (RuntimeException terminalFailure) {
                LOG.error("Failed to publish executor-failure terminal for turnId={}", context.turnId(), terminalFailure);
            }
        }
    }

    private TurnRecord recoverOrphan(TurnRecord running) {
        var bus = buses.computeIfAbsent(running.turnId(),
            ignored -> new TurnEventBus(running.turnId(), running.sessionId()));
        var sink = new TurnEventSink(running.turnId(), running.sessionId(), running.lastSeq(), store, bus,
            objectMapper, clock);
        sink.publishTerminal(EventType.FAILED, TurnStatus.FAILED_ORPHANED, "turn-runtime", "orphan-recovery",
            "Running turn marked orphaned after application restart.", Map.of("code", "TURN_ORPHANED"));
        return store.findTurn(running.turnId()).orElse(running);
    }

    private TurnEventSink sinkFor(String turnId, String sessionId, long lastSeq) {
        var bus = buses.computeIfAbsent(turnId, ignored -> new TurnEventBus(turnId, sessionId));
        return new TurnEventSink(turnId, sessionId, lastSeq, store, bus, objectMapper, clock);
    }

    private TurnEventBus requireBus(String turnId) {
        var bus = buses.get(turnId);
        if (bus == null) {
            var stored = store.findTurn(turnId).orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
            if (stored.status() == TurnStatus.RUNNING) {
                stored = recoverOrphan(stored);
            }
            var record = stored;
            if (record.status().isTerminal()) {
                var recreated = new TurnEventBus(turnId, record.sessionId());
                store.findLastEvent(turnId).filter(event -> event.type().isTerminal())
                    .ifPresent(recreated::markTerminal);
                recreated.close();
                return recreated;
            }
            bus = buses.computeIfAbsent(turnId, ignored -> {
                var recreated = new TurnEventBus(turnId, record.sessionId());
                recreated.terminalFuture().whenComplete((terminal, error) -> buses.remove(turnId, recreated));
                return recreated;
            });
        }
        return bus;
    }

    private void validateCapability(String capability) {
        if (capability == null || capability.isBlank() || !supportedCapabilities.contains(capability)) {
            throw new TurnApiException(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN);
        }
    }

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    public record StartTurnOutcome(TurnRecord record, TurnEventBus bus) {
        public StartTurnOutcome {
            if (record == null || bus == null) {
                throw new IllegalArgumentException("record and bus are required");
            }
        }
    }

    public record ActiveTurnInfo(String sessionId, String turnId, TurnStatus status) {
        public ActiveTurnInfo {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("sessionId is required");
            }
            if (turnId != null && turnId.isBlank()) {
                throw new IllegalArgumentException("turnId must be null or non-blank");
            }
        }

        public static ActiveTurnInfo none(String sessionId) {
            return new ActiveTurnInfo(sessionId, null, null);
        }
    }
}

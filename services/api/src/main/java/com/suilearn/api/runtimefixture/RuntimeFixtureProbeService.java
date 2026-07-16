package com.suilearn.api.runtimefixture;

import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.persistence.entity.DeadLetterMessageEntity;
import com.suilearn.api.persistence.repository.DeadLetterMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import com.suilearn.api.task.application.DeadLetterReplayService;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Executes profile-only, content-free fault probes and returns opaque verification flags. */
@Service
@Profile("runtime-fixture")
public class RuntimeFixtureProbeService {
    private static final String STAGE = "RUNTIME_FIXTURE";
    private final RuntimeFixtureControl control;
    private final RuntimeFixtureAiProvider ai;
    private final TesseractOcrAdapter ocr;
    private final TaskOutboxSubmissionService submissions;
    private final TaskService tasks;
    private final DeadLetterReplayService deadLetters;
    private final OutboxEventJpaRepository outbox;
    private final DeadLetterMessageJpaRepository deadLetterMessages;
    private final Clock clock;
    private final RuntimeFixtureDuplicateMessageProbe duplicateMessages;
    private final RuntimeFixtureDeletionCleanupProbe deletionCleanup;

    @Autowired
    public RuntimeFixtureProbeService(
        RuntimeFixtureControl control, RuntimeFixtureAiProvider ai, TesseractOcrAdapter ocr,
        TaskOutboxSubmissionService submissions, TaskService tasks, DeadLetterReplayService deadLetters,
        OutboxEventJpaRepository outbox, DeadLetterMessageJpaRepository deadLetterMessages, Clock clock,
        RuntimeFixtureDuplicateMessageProbe duplicateMessages, RuntimeFixtureDeletionCleanupProbe deletionCleanup
    ) {
        this.control = control;
        this.ai = ai;
        this.ocr = ocr;
        this.submissions = submissions;
        this.tasks = tasks;
        this.deadLetters = deadLetters;
        this.outbox = outbox;
        this.deadLetterMessages = deadLetterMessages;
        this.clock = clock;
        this.duplicateMessages = duplicateMessages;
        this.deletionCleanup = deletionCleanup;
    }

    RuntimeFixtureProbeService(
        RuntimeFixtureControl control, RuntimeFixtureAiProvider ai, RuntimeFixtureProcessRunner processRunner,
        TaskOutboxSubmissionService submissions, TaskService tasks, DeadLetterReplayService deadLetters,
        OutboxEventJpaRepository outbox, DeadLetterMessageJpaRepository deadLetterMessages, Clock clock,
        RuntimeFixtureDuplicateMessageProbe duplicateMessages, RuntimeFixtureDeletionCleanupProbe deletionCleanup
    ) {
        this(control, ai, new TesseractOcrAdapter("tesseract", processRunner, 1, Duration.ofMillis(1), "runtime-fixture-v1"),
            submissions, tasks, deadLetters, outbox, deadLetterMessages, clock, duplicateMessages, deletionCleanup);
    }

    @Transactional
    public Object trigger(String rawKind) {
        ProbeKind kind = parse(rawKind);
        if (kind == ProbeKind.DUPLICATE_MESSAGE) return duplicateMessages.trigger();
        if (kind == ProbeKind.DELETION_CLEANUP) return deletionCleanup.trigger();
        requireFaultEnabled(kind);
        boolean workTimedOut = executeWork(kind);
        if (!workTimedOut) throw new ResponseStatusException(HttpStatus.CONFLICT, "Runtime fixture work did not time out");

        var task = submissions.submit(
            TaskKind.EXPLANATION_GENERATION,
            "runtime-fixture", "runtime-fixture", null, null, "RUNTIME_FIXTURE", STAGE,
            "runtime-fixture:" + UUID.randomUUID(), "{}"
        );
        tasks.updateTask(task, TaskLifecycleStatus.FAILED, 100, kind + "_TIMEOUT", null, kind + "_TIMEOUT", "runtime fixture timeout", null, null);
        var original = outbox.findAll().stream().filter(event -> task.id().equals(event.taskId()) && STAGE.equals(event.stage()))
            .findFirst().orElseThrow(() -> new IllegalStateException("Runtime fixture Outbox record was not persisted"));
        deadLetterMessages.save(DeadLetterMessageEntity.recorded(
            original.id(), task.id(), STAGE, "runtime-fixture", 0, "TRANSIENT", kind + "_TIMEOUT", clock.instant()
        ));
        original.markDeadLetter();
        outbox.save(original);
        deadLetters.replay(original.id());

        boolean taskRetryPersisted = tasks.getTaskStatus(task.id()).retryCount() == 1;
        boolean deadLetterRecorded = deadLetterMessages.existsById(original.id());
        var taskEvents = outbox.findAll().stream()
            .filter(event -> task.id().equals(event.taskId()) && STAGE.equals(event.stage()))
            .toList();
        var persistedOriginal = taskEvents.stream()
            .filter(event -> original.id().equals(event.id()))
            .findFirst();
        boolean originalOutboxDispatchPrevented = persistedOriginal.isPresent()
            && "DEAD_LETTER".equals(persistedOriginal.get().state());
        var pendingTaskEvents = taskEvents.stream()
            .filter(event -> "PENDING".equals(event.state()))
            .toList();
        boolean exclusiveReplayOutboxPersisted = pendingTaskEvents.size() == 1
            && !original.id().equals(pendingTaskEvents.getFirst().id())
            && pendingTaskEvents.getFirst().retryCount() == 1;
        return new FixtureProbeResponse(
            workTimedOut, taskRetryPersisted, deadLetterRecorded,
            originalOutboxDispatchPrevented, exclusiveReplayOutboxPersisted
        );
    }

    private boolean executeWork(ProbeKind kind) {
        if (kind == ProbeKind.OCR) {
            return "TIMED_OUT".equals(ocr.recognize(Path.of("runtime-fixture-input"), "runtime-fixture", 1).status());
        }
        try {
            ai.extractKnowledgePoints(new com.suilearn.api.ai.AiProvider.KnowledgePointExtractionPrompt(
                "runtime-fixture", "runtime-fixture", "runtime-fixture", List.of(), 1
            ));
            return false;
        } catch (RuntimeException expected) {
            return true;
        }
    }

    private void requireFaultEnabled(ProbeKind kind) {
        RuntimeFixtureControl.Mode mode = kind == ProbeKind.OCR ? control.ocrMode() : control.aiMode();
        if (mode != RuntimeFixtureControl.Mode.TIMEOUT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Enable the requested runtime fixture timeout before probing");
        }
    }

    private ProbeKind parse(String rawKind) {
        try { return ProbeKind.valueOf(rawKind.replace('-', '_').toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported runtime fixture probe"); }
    }

    private enum ProbeKind { OCR, AI, DUPLICATE_MESSAGE, DELETION_CLEANUP }

    public record FixtureProbeResponse(
        boolean workTimedOut, boolean taskRetryPersisted, boolean deadLetterRecorded,
        boolean originalOutboxDispatchPrevented, boolean exclusiveReplayOutboxPersisted
    ) { }
}

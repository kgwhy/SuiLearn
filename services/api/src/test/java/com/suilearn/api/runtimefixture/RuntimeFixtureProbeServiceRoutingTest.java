package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.DeadLetterMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import com.suilearn.api.task.application.DeadLetterReplayService;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeFixtureProbeServiceRoutingTest {
    @Test
    void routesOpaqueDuplicateAndDeletionProbesWithoutRequiringFaultModes() {
        var duplicate = mock(RuntimeFixtureDuplicateMessageProbe.class);
        var deletion = mock(RuntimeFixtureDeletionCleanupProbe.class);
        var duplicateResponse = new RuntimeFixtureDuplicateMessageProbe.DuplicateMessageProbeResponse(true, true);
        var deletionResponse = new RuntimeFixtureDeletionCleanupProbe.DeletionCleanupProbeResponse(true, true);
        when(duplicate.trigger()).thenReturn(duplicateResponse);
        when(deletion.trigger()).thenReturn(deletionResponse);
        var probes = new RuntimeFixtureProbeService(
            new RuntimeFixtureControl(), mock(RuntimeFixtureAiProvider.class), mock(RuntimeFixtureProcessRunner.class),
            mock(TaskOutboxSubmissionService.class), mock(TaskService.class), mock(DeadLetterReplayService.class),
            mock(OutboxEventJpaRepository.class), mock(DeadLetterMessageJpaRepository.class), Clock.systemUTC(), duplicate, deletion
        );

        assertThat(probes.trigger("duplicate-message")).isSameAs(duplicateResponse);
        assertThat(probes.trigger("deletion-cleanup")).isSameAs(deletionResponse);
    }

    @Test
    void deadLetterReplayMakesTheOriginalOutboxEventNonDispatchableAndCreatesOneReplay() {
        var control = new RuntimeFixtureControl();
        control.setAiMode(RuntimeFixtureControl.Mode.TIMEOUT);
        var ai = mock(RuntimeFixtureAiProvider.class);
        when(ai.extractKnowledgePoints(any())).thenThrow(new RuntimeException("timeout"));
        var submissions = mock(TaskOutboxSubmissionService.class);
        var tasks = mock(TaskService.class);
        var outbox = mock(OutboxEventJpaRepository.class);
        var deadLetters = mock(DeadLetterReplayService.class);
        var deadLetterMessages = mock(DeadLetterMessageJpaRepository.class);
        var createdTask = task("task_fixture", 0);
        var retriedTask = task("task_fixture", 1);
        var original = OutboxEventEntity.pending(
            "outbox_original", "task_fixture", "RUNTIME_FIXTURE", "fixture-original", "{}", Instant.EPOCH
        );
        var replay = OutboxEventEntity.pending(
            "outbox_replay", "task_fixture", "RUNTIME_FIXTURE", "fixture-replay", "{}", Instant.EPOCH, 1
        );
        when(submissions.submit(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(createdTask);
        when(tasks.getTaskStatus("task_fixture")).thenReturn(retriedTask);
        when(outbox.findAll()).thenReturn(List.of(original), List.of(original, replay));
        when(deadLetterMessages.existsById("outbox_original")).thenReturn(true);
        var probes = new RuntimeFixtureProbeService(
            control, ai, mock(RuntimeFixtureProcessRunner.class), submissions, tasks, deadLetters, outbox,
            deadLetterMessages, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), mock(RuntimeFixtureDuplicateMessageProbe.class),
            mock(RuntimeFixtureDeletionCleanupProbe.class)
        );

        var response = (RuntimeFixtureProbeService.FixtureProbeResponse) probes.trigger("ai");

        assertThat(response.getClass().getRecordComponents()).extracting(component -> component.getName())
            .contains("originalOutboxDispatchPrevented", "exclusiveReplayOutboxPersisted");
        assertThat(original.state()).isEqualTo("DEAD_LETTER");
        verify(outbox).save(original);
        verify(deadLetters).replay("outbox_original");
    }

    private TaskStatus task(String id, int retryCount) {
        return new TaskStatus(
            id, TaskKind.EXPLANATION_GENERATION, TaskLifecycleStatus.FAILED, "runtime-fixture", "runtime-fixture", null,
            AiProviderType.OPENAI_COMPATIBLE, "runtime-fixture", 0, "RUNTIME_FIXTURE", "FAILED", "safe", retryCount,
            null, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH
        );
    }
}

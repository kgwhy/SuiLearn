package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmUsage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemoryConsolidatorTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void duplicateCommandIsIdempotent() {
        var commands = mock(MemoryConsolidationCommandRepository.class);
        var existing = new MemoryConsolidationCommandEntity("c1", "learner", "answer", "update",
            "learner:answer:update", "PROCESSED", NOW, NOW);
        when(commands.findByIdempotencyKey("learner:answer:update")).thenReturn(Optional.of(existing));
        var consolidator = new MemoryConsolidator(commands, mock(MemorySnapshotRepository.class),
            mock(MemoryL2DocRepository.class), mock(MemoryL3DocRepository.class),
            mock(MemoryMetaRepository.class), mock(LlmClient.class),
            JsonMapper.builder().findAndAddModules().build(), "fake", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(consolidator.submitUpdate("learner", "answer", "update")).isSameAs(existing);
        verify(commands, never()).save(any());
    }

    @Test
    void processDueConsumesSnapshotAndMarksCommandProcessed() {
        var commands = mock(MemoryConsolidationCommandRepository.class);
        var snapshots = mock(MemorySnapshotRepository.class);
        var l2 = mock(MemoryL2DocRepository.class);
        var meta = mock(MemoryMetaRepository.class);
        var command = new MemoryConsolidationCommandEntity("c1", "learner", "answer", "update",
            "idem", "PENDING", NOW, null);
        var snap = new MemorySnapshotEntity("s1", "learner", "answer", "q1", "fact", "fp", NOW, false);
        when(commands.findTop10ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(command));
        when(snapshots.findByLearnerIdAndConsumedFalseOrderByCreatedAtAsc("learner")).thenReturn(List.of(snap));
        when(l2.findByLearnerIdAndSurface("learner", "answer")).thenReturn(Optional.empty());
        var client = mock(LlmClient.class);
        when(client.chat(any())).thenReturn(new LlmResponse("## answer\n- fact", List.of(), new LlmUsage(2, 2), "stop"));
        var consolidator = new MemoryConsolidator(commands, snapshots, l2, mock(MemoryL3DocRepository.class),
            meta, client, JsonMapper.builder().findAndAddModules().build(), "fake",
            Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(consolidator.processDue()).isEqualTo(1);
        assertThat(command.getStatus()).isEqualTo("PROCESSED");
        assertThat(snap.isConsumed()).isTrue();
        verify(l2).save(any());
        verify(commands).save(command);
    }

    @Test
    void noSnapshotDoesNotCallLlm() {
        var commands = mock(MemoryConsolidationCommandRepository.class);
        var snapshots = mock(MemorySnapshotRepository.class);
        var command = new MemoryConsolidationCommandEntity("c1", "learner", "answer", "update",
            "idem", "PENDING", NOW, null);
        when(commands.findTop10ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(command));
        when(snapshots.findByLearnerIdAndConsumedFalseOrderByCreatedAtAsc("learner")).thenReturn(List.of());
        var client = mock(LlmClient.class);
        var consolidator = new MemoryConsolidator(commands, snapshots, mock(MemoryL2DocRepository.class),
            mock(MemoryL3DocRepository.class), mock(MemoryMetaRepository.class), client,
            JsonMapper.builder().findAndAddModules().build(), "fake", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(consolidator.processDue()).isEqualTo(1);
        verify(client, never()).chat(any());
    }
}

package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.agent.runtime.SourceSelection;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MemoryTurnRecorderTest {
    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void completedTurnRecordsTraceSnapshotAndCommand() {
        var traces = mock(MemoryTraceRecorder.class);
        var snapshots = mock(MemorySnapshotRecorder.class);
        var consolidator = mock(MemoryConsolidator.class);
        when(snapshots.record(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        var recorder = new MemoryTurnRecorder(traces, snapshots, consolidator, mapper);

        recorder.recordTerminalTurn(context(), "COMPLETED", 2, new LlmUsage(10, 4), "final answer");

        verify(traces).append(eq("learner-1"), eq("turn-1"), eq("turn"), eq("turn_completed"),
            contains("\"status\":\"COMPLETED\""));
        verify(snapshots).record(eq("learner-1"), eq("turn"), eq("turn:turn-1"),
            contains("final answer"), anyString());
        verify(consolidator).submitUpdate("learner-1", "turn", "turn-1");
    }

    @Test
    void failedTurnRecordsOnlyTrace() {
        var traces = mock(MemoryTraceRecorder.class);
        var snapshots = mock(MemorySnapshotRecorder.class);
        var consolidator = mock(MemoryConsolidator.class);
        var recorder = new MemoryTurnRecorder(traces, snapshots, consolidator, mapper);

        recorder.recordTerminalTurn(context(), "BUDGET_EXHAUSTED", 4, new LlmUsage(10, 4), "");

        verify(traces).append(eq("learner-1"), eq("turn-1"), eq("turn"), eq("turn_budget_exhausted"),
            contains("\"status\":\"BUDGET_EXHAUSTED\""));
        verify(snapshots, never()).record(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(consolidator, never()).submitUpdate(anyString(), anyString(), anyString());
    }

    @Test
    void duplicateSnapshotDoesNotSubmitCommand() {
        var traces = mock(MemoryTraceRecorder.class);
        var snapshots = mock(MemorySnapshotRecorder.class);
        var consolidator = mock(MemoryConsolidator.class);
        when(snapshots.record(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(false);
        var recorder = new MemoryTurnRecorder(traces, snapshots, consolidator, mapper);

        recorder.recordTerminalTurn(context(), "COMPLETED", 0, LlmUsage.none(), "answer");

        verify(snapshots).record(eq("learner-1"), eq("turn"), eq("turn:turn-1"), anyString(), anyString());
        verify(consolidator, never()).submitUpdate(anyString(), anyString(), anyString());
    }

    @Test
    void resultExcerptIsBounded() throws Exception {
        var traces = mock(MemoryTraceRecorder.class);
        var snapshots = mock(MemorySnapshotRecorder.class);
        var consolidator = mock(MemoryConsolidator.class);
        when(snapshots.record(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        var recorder = new MemoryTurnRecorder(traces, snapshots, consolidator, mapper);
        String longAnswer = "x".repeat(3000);

        recorder.recordTerminalTurn(context(), "COMPLETED", 0, LlmUsage.none(), longAnswer);

        var payload = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(snapshots).record(eq("learner-1"), eq("turn"), eq("turn:turn-1"), payload.capture(), anyString());
        var json = mapper.readTree(payload.getValue());
        assertThat(json.path("resultExcerpt").asText()).hasSize(1200);
    }

    private TurnContext context() {
        return new TurnContext("turn-1", "sess-1", "learner-1", "study_agent",
            new StudyScope("kb-1", null), List.<SourceSelection>of(), "question", List.of(), List.of(), Map.of());
    }
}

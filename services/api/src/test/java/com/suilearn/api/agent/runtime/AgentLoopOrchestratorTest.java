package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.agent.loop.AgentLoop;
import com.suilearn.api.agent.loop.ScriptedLlmClient;
import com.suilearn.api.agent.loop.ToolDispatcher;
import com.suilearn.api.agent.memory.MemoryTurnRecorder;
import com.suilearn.api.agent.tool.AskUserTool;
import com.suilearn.api.agent.tool.Tool;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AgentLoopOrchestratorTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Test
    void studyAgentUsesLoopAndCompletesInsteadOfUnavailable() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var tools = new ToolRegistry(Map.of("ask_user", new AskUserTool()));
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("Loop answer.", List.of(), new LlmUsage(3, 2), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, mapper), tools,
            new AgentConfigurationProperties(true, 4, 3, 8, Duration.ofSeconds(10), 12000, 3,
                new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
                new AgentConfigurationProperties.Memory(5, 0.8)),
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model");
        var orchestrator = new TurnOrchestrator(CapabilityRegistry.builtin(), loop);
        var service = new TurnRuntimeService(new InMemoryTurnStore(), orchestrator, mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner", "sess", "question", null,
            new StudyScope("kb", null), List.of(), List.of()));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(result.terminalEvent().type()).isEqualTo(EventType.DONE);
        assertThat(service.eventsAfter(result.turnId(), 0).events()).extracting(StreamEvent::content)
            .contains("Loop answer.");
    }

    @Test
    void ragQaUsesLoopAndCompletes() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var tools = new ToolRegistry(Map.of("ask_user", new AskUserTool()));
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("Evidence answer.", List.of(), new LlmUsage(4, 2), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, mapper), tools,
            new AgentConfigurationProperties(true, 4, 3, 8, Duration.ofSeconds(10), 12000, 3,
                new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
                new AgentConfigurationProperties.Memory(5, 0.8)),
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model");
        var service = new TurnRuntimeService(new InMemoryTurnStore(),
            new TurnOrchestrator(CapabilityRegistry.builtin(), loop), mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner", "sess", "question", "rag_qa",
            new StudyScope("kb", null), List.of(), List.of()));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(service.eventsAfter(result.turnId(), 0).events()).extracting(StreamEvent::content)
            .contains("Evidence answer.");
    }

    @Test
    void questionGenerationUsesLoopAndCompletes() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var tools = new ToolRegistry(Map.of("ask_user", new AskUserTool()));
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("Practice draft.", List.of(), new LlmUsage(4, 2), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, mapper), tools,
            new AgentConfigurationProperties(true, 4, 3, 8, Duration.ofSeconds(10), 12000, 3,
                new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
                new AgentConfigurationProperties.Memory(5, 0.8)),
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model");
        var service = new TurnRuntimeService(new InMemoryTurnStore(),
            new TurnOrchestrator(CapabilityRegistry.builtin(), loop), mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner", "sess", "generate", "question_generation",
            new StudyScope("kb", null), List.of(), List.of()));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(service.eventsAfter(result.turnId(), 0).events()).extracting(StreamEvent::content)
            .contains("Practice draft.");
    }

    @Test
    void memoryRecordingFailureDoesNotChangeTerminalOutcome() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var tools = new ToolRegistry(Map.of("ask_user", new AskUserTool()));
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("Loop answer.", List.of(), new LlmUsage(3, 2), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, mapper), tools,
            new AgentConfigurationProperties(true, 4, 3, 8, Duration.ofSeconds(10), 12000, 3,
                new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
                new AgentConfigurationProperties.Memory(5, 0.8)),
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model");
        var memory = mock(MemoryTurnRecorder.class);
        doThrow(new IllegalStateException("memory unavailable")).when(memory).recordTerminalTurn(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString());
        var service = new TurnRuntimeService(new InMemoryTurnStore(),
            new TurnOrchestrator(CapabilityRegistry.builtin(), loop, memory), mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        var outcome = service.start(new StartTurnCommand("learner", "sess", "question", null,
            new StudyScope("kb", null), List.of(), List.of()));
        TurnResult result = service.awaitResult(outcome.record().turnId(), Duration.ofSeconds(2));

        assertThat(result.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(result.terminalEvent().type()).isEqualTo(EventType.DONE);
        verify(memory, timeout(2000)).recordTerminalTurn(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("COMPLETED"), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void unknownCapabilityStillFailsBeforeLoop() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var tools = new ToolRegistry(Map.of("ask_user", new AskUserTool()));
        var client = new ScriptedLlmClient(List.of(), new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, mapper), tools,
            new AgentConfigurationProperties(true, 4, 3, 8, Duration.ofSeconds(10), 12000, 3,
                new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
                new AgentConfigurationProperties.Memory(5, 0.8)),
            Clock.fixed(NOW, ZoneOffset.UTC), "fake-model");
        var service = new TurnRuntimeService(new InMemoryTurnStore(),
            new TurnOrchestrator(CapabilityRegistry.builtin(), loop), mapper,
            Clock.fixed(NOW, ZoneOffset.UTC), Set.of("study_agent", "rag_qa", "question_generation"),
            Executors.newVirtualThreadPerTaskExecutor());

        assertThatThrownBy(() -> service.start(new StartTurnCommand("learner", "sess", "question",
            "unknown_capability", new StudyScope("kb", null), List.of(), List.of())))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_CAPABILITY_UNKNOWN));
    }
}

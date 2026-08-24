package com.suilearn.api.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.learner.LearnerProfile;
import com.suilearn.api.agent.learner.LearnerProfileService;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.agent.runtime.TurnReply;
import com.suilearn.api.agent.runtime.TurnReplyChannel;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgentLoopTest {
    @Test
    void completesToolLoopAndPublishesResult() {
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}")),
                LlmUsage.none(), "tool_calls"),
            new LlmResponse("Hooks need practice.", List.of(), new LlmUsage(10, 5), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.searchTools(request -> List.of());
        var loop = new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(4, 8), java.time.Clock.fixed(LoopFixtures.NOW, java.time.ZoneOffset.UTC),
            "fake-model");
        var sink = LoopFixtures.sink(null);

        LoopResult result = loop.run(LoopFixtures.context("study_agent"), LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.COMPLETED);
        assertThat(result.content()).isEqualTo("Hooks need practice.");
        assertThat(result.toolCalls()).isEqualTo(1);
        assertThat(sink.store().findEventsAfter("turn-loop", 0)).extracting(event -> event.type().name())
            .contains("TOOL_CALL", "TOOL_RESULT", "RESULT", "DONE");
    }

    @Test
    void emptyAnswersNudgeThenFailClosed() {
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.searchTools(request -> List.of());
        var loop = new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(4, 8), java.time.Clock.fixed(LoopFixtures.NOW, java.time.ZoneOffset.UTC),
            "fake-model");
        var sink = LoopFixtures.sink(null);

        LoopResult result = loop.run(LoopFixtures.context("study_agent"), LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.INVALID_MODEL_OUTPUT);
        assertThat(sink.store().findEventsAfter("turn-loop", 0)).extracting(event -> event.type().name())
            .contains("ERROR", "FAILED");
    }

    @Test
    void learnerProfileIsInjectedIntoSystemPrompt() {
        var profiles = Mockito.mock(LearnerProfileService.class);
        Mockito.when(profiles.get("learner-loop")).thenReturn(Optional.of(
            new LearnerProfile("learner-loop", "visual learner", List.of("Java", "Spring"))));
        var captured = new AtomicReference<com.suilearn.api.agent.llm.LlmRequest>();
        var client = new com.suilearn.api.agent.llm.LlmClient() {
            @Override public java.util.stream.Stream<com.suilearn.api.agent.llm.LlmChunk> stream(
                com.suilearn.api.agent.llm.LlmRequest request) { return java.util.stream.Stream.of(); }
            @Override public LlmResponse chat(com.suilearn.api.agent.llm.LlmRequest request) {
                captured.set(request);
                return new LlmResponse("Answer.", List.of(), new LlmUsage(3, 2), "stop");
            }
        };
        var tools = LoopFixtures.searchTools(request -> List.of());
        var builder = new com.suilearn.api.agent.context.ContextBuilder(
            com.suilearn.api.agent.context.TokenEstimator.conservativeCharacters(),
            new com.suilearn.api.agent.context.PromptBlockAssembler(
                com.suilearn.api.agent.context.TokenEstimator.conservativeCharacters()), 12000);
        var loop = new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(4, 8), java.time.Clock.fixed(LoopFixtures.NOW, java.time.ZoneOffset.UTC),
            "fake-model", builder, null, null, null, profiles);

        loop.run(LoopFixtures.context("study_agent"), LoopFixtures.studyManifest(), LoopFixtures.sink(null).sink());

        assertThat(captured.get().messages().getFirst().content())
            .contains("Learner persona: visual learner", "Learner skills: Java, Spring");
    }

    @Test
    void budgetStopsBeforeAdditionalToolExecution() {
        var call = new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}");
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls"),
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.searchTools(request -> List.of());
        var loop = new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(4, 1), java.time.Clock.fixed(LoopFixtures.NOW, java.time.ZoneOffset.UTC),
            "fake-model");
        var sink = LoopFixtures.sink(null);

        LoopResult result = loop.run(LoopFixtures.context("study_agent"), LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.BUDGET_EXHAUSTED);
    }

    @Test
    void askUserPausesAndResumesInPlace() throws Exception {
        var ask = new LlmToolCall("ask", "ask_user",
            "{\"questionId\":\"q1\",\"prompt\":\"Which level?\"}");
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(ask), LlmUsage.none(), "tool_calls"),
            new LlmResponse("Resumed answer.", List.of(), new LlmUsage(5, 2), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = new com.suilearn.api.agent.runtime.ToolRegistry(
            java.util.Map.of("ask_user", new com.suilearn.api.agent.tool.AskUserTool()));
        var loop = new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(4, 8), java.time.Clock.fixed(LoopFixtures.NOW, java.time.ZoneOffset.UTC),
            "fake-model");
        var replies = new LinkedBlockingQueue<TurnReply>();
        TurnReplyChannel channel = (turnId, timeout) -> replies.poll(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        var sink = LoopFixtures.sink(channel);

        var thread = new Thread(() -> loop.run(LoopFixtures.context("study_agent"),
            LoopFixtures.studyManifest(), sink.sink()));
        thread.start();
        Thread.sleep(150);
        assertThat(sink.store().findTurn("turn-loop").orElseThrow().status().name()).isEqualTo("WAITING_INPUT");
        replies.put(new TurnReply("EASY", java.util.Map.of()));
        thread.join(3000);
        assertThat(thread.isAlive()).isFalse();
        assertThat(sink.store().findTurn("turn-loop").orElseThrow().status().name()).isEqualTo("COMPLETED");
    }
}

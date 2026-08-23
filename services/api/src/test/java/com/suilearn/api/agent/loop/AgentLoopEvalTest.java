package com.suilearn.api.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.llm.LlmUsage;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentLoopEvalTest {
    @Test
    void eval01ToolLoopCompletesWithResult() {
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}")),
                LlmUsage.none(), "tool_calls"),
            new LlmResponse("Study hooks from evidence.", List.of(), new LlmUsage(8, 4), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.searchTools(request -> List.of());
        var sink = LoopFixtures.sink(null);

        var result = loop(client, tools).run(LoopFixtures.context("study_agent"),
            LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.COMPLETED);
        assertThat(sink.store().countEvents("turn-loop")).isEqualTo(7);
    }

    @Test
    void eval02EmptyAnswerNudgeFailsClosed() {
        var blank = new LlmResponse("", List.of(), LlmUsage.none(), "stop");
        var client = new ScriptedLlmClient(List.of(blank, blank, blank), blank);
        var tools = LoopFixtures.searchTools(request -> List.of());
        var sink = LoopFixtures.sink(null);

        var result = loop(client, tools).run(LoopFixtures.context("study_agent"),
            LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.INVALID_MODEL_OUTPUT);
    }

    @Test
    void eval03BudgetStopsToolExecution() {
        var call = new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}");
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls"),
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.searchTools(request -> List.of());
        var sink = LoopFixtures.sink(null);

        var result = loop(client, tools, 4, 1).run(LoopFixtures.context("study_agent"),
            LoopFixtures.studyManifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.BUDGET_EXHAUSTED);
    }

    private AgentLoop loop(ScriptedLlmClient client, com.suilearn.api.agent.runtime.ToolRegistry tools) {
        return loop(client, tools, 4, 8);
    }

    private AgentLoop loop(ScriptedLlmClient client, com.suilearn.api.agent.runtime.ToolRegistry tools,
                           int steps, int maxTools) {
        return new AgentLoop(client, new ToolDispatcher(tools, LoopFixtures.MAPPER), tools,
            LoopFixtures.properties(steps, maxTools),
            Clock.fixed(LoopFixtures.NOW, ZoneOffset.UTC), "fake-model");
    }
}

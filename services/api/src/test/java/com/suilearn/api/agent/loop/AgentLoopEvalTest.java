package com.suilearn.api.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.llm.LlmResponse;
import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.llm.LlmUsage;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.tool.GeneratePracticeTool;
import com.suilearn.api.agent.tool.PracticeCoachSubAgent;
import com.suilearn.api.agent.tool.PracticeModelPort;
import com.suilearn.api.agent.tool.SearchKnowledgeTool;
import com.suilearn.api.agent.tool.TemporaryExercise;
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
        assertThat(sink.store().countEvents("turn-loop")).isEqualTo(8);
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
    void eval04RagQaCompletesSearchLoopWithRagManifest() {
        var call = new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}");
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls"),
            new LlmResponse("Evidence only answer.", List.of(), new LlmUsage(6, 3), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var tools = LoopFixtures.tools(Map.of("search_knowledge",
            new SearchKnowledgeTool(request -> List.of())));
        var sink = LoopFixtures.sink(null);

        var result = loop(client, tools).run(LoopFixtures.context("rag_qa"),
            BuiltinCapabilities.ragQa().manifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.COMPLETED);
        assertThat(sink.store().findEventsAfter("turn-loop", 0))
            .extracting(StreamEvent::type).contains(EventType.TOOL_CALL, EventType.TOOL_RESULT, EventType.RESULT);
    }

    @Test
    void eval05QuestionGenerationCompletesGeneratePracticeLoop() {
        var call = new LlmToolCall("c1", "generate_practice",
            "{\"learningGoal\":\"java\",\"difficulty\":\"EASY\",\"practiceCount\":1,\"evidence\":[{\"stableId\":\"s1\",\"sourceRef\":\"ref-1\",\"content\":\"evidence\",\"relevance\":0.8}]}");
        var client = new ScriptedLlmClient(List.of(
            new LlmResponse("", List.of(call), LlmUsage.none(), "tool_calls"),
            new LlmResponse("Temporary practice generated.", List.of(), new LlmUsage(6, 3), "stop")),
            new LlmResponse("", List.of(), LlmUsage.none(), "stop"));
        var model = (PracticeModelPort) request -> new PracticeModelPort.Draft(
            "explanation", List.of(new TemporaryExercise("q", "a", "why", List.of("ref-1"))),
            List.of("ref-1"), "next");
        var tools = LoopFixtures.tools(Map.of("generate_practice",
            new GeneratePracticeTool(new PracticeCoachSubAgent(model))));
        var sink = LoopFixtures.sink(null);

        var result = loop(client, tools).run(LoopFixtures.context("question_generation"),
            BuiltinCapabilities.questionGeneration().manifest(), sink.sink());

        assertThat(result.status()).isEqualTo(LoopResult.Status.COMPLETED);
        assertThat(sink.store().findEventsAfter("turn-loop", 0))
            .extracting(StreamEvent::type).contains(EventType.TOOL_CALL, EventType.TOOL_RESULT, EventType.RESULT);
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

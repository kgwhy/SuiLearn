package com.suilearn.api.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.llm.LlmToolCall;
import com.suilearn.api.agent.tool.AskUserTool;
import com.suilearn.api.agent.tool.EvidencePointer;
import com.suilearn.api.agent.tool.SearchKnowledgeTool;
import com.suilearn.api.agent.tool.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolDispatcherTest {
    @Test
    void duplicatesExecuteOnceAndMissingArgumentsAreReported() {
        AtomicInteger calls = new AtomicInteger();
        var tools = LoopFixtures.searchTools(request -> {
            calls.incrementAndGet();
            return List.of(new EvidencePointer("stable", "source", "kb-loop", "mat-loop", 0.8d));
        });
        var dispatcher = new ToolDispatcher(tools, LoopFixtures.MAPPER);
        var sink = LoopFixtures.sink(null);

        var executions = dispatcher.dispatch(LoopFixtures.context("study_agent"), LoopFixtures.studyManifest(),
            List.of(new LlmToolCall("c1", "search_knowledge", "{\"query\":\"hooks\"}"),
                new LlmToolCall("c2", "search_knowledge", "{\"query\":\"hooks\"}"),
                new LlmToolCall("c3", "search_knowledge", "{}")),
            sink.sink());

        assertThat(calls).hasValue(1);
        assertThat(executions).hasSize(2);
        assertThat(executions.stream().filter(item -> item.result().success())).hasSize(1);
        assertThat(executions.stream().filter(item ->
            "MISSING_REQUIRED_ARGUMENT".equals(item.result().metadata().get("code")))).hasSize(1);
    }

    @Test
    void forbiddenToolIsRejectedWithoutExecution() {
        var tools = new com.suilearn.api.agent.runtime.ToolRegistry(
            Map.of("ask_user", new AskUserTool()));
        var dispatcher = new ToolDispatcher(tools, LoopFixtures.MAPPER);
        var sink = LoopFixtures.sink(null);

        var executions = dispatcher.dispatch(LoopFixtures.context("rag_qa"),
            BuiltinCapabilities.ragQa().manifest(),
            List.of(new LlmToolCall("c1", "ask_user", "{}")), sink.sink());

        assertThat(executions).singleElement().satisfies(execution ->
            assertThat(execution.result().metadata()).containsEntry("code", "FORBIDDEN_AGENT_ACTION"));
    }

    @Test
    void askUserRaisesPauseSignal() {
        var tools = new com.suilearn.api.agent.runtime.ToolRegistry(
            Map.of("ask_user", new AskUserTool()));
        var dispatcher = new ToolDispatcher(tools, LoopFixtures.MAPPER);
        var sink = LoopFixtures.sink(null);

        assertThatThrownBy(() -> dispatcher.dispatch(LoopFixtures.context("study_agent"),
            LoopFixtures.studyManifest(),
            List.of(new LlmToolCall("c1", "ask_user", "{\"questionId\":\"q1\",\"prompt\":\"Which level?\"}")),
            sink.sink()))
            .isInstanceOfSatisfying(AskUserPauseException.class, pause ->
                assertThat(pause.payload().questionId()).isEqualTo("q1"));
    }
}

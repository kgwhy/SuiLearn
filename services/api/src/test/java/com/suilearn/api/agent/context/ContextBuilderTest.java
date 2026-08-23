package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextBuilderTest {
    @Test
    void overBudgetTrimsOldToolMessagesAndInsertsMarker() {
        var estimator = TokenEstimator.conservativeCharacters();
        var builder = new ContextBuilder(estimator,
            new PromptBlockAssembler(estimator), 20);
        var context = new TurnContext("turn", "sess", "learner", "study_agent",
            new StudyScope("kb", null), List.of(), "current question", List.of(), List.of(), Map.of());
        var history = List.of(
            LlmMessage.user("old user question"),
            LlmMessage.assistant("old assistant answer", List.of()),
            LlmMessage.tool("t1", "old tool result"),
            LlmMessage.user("recent user question"));

        var built = builder.build(context, BuiltinCapabilities.studyAgent().manifest(), history);

        assertThat(built.trimmedMessages()).isGreaterThanOrEqualTo(1);
        assertThat(built.messages().stream().filter(message -> "tool".equals(message.role()))).isEmpty();
        assertThat(built.messages().getFirst().role()).isEqualTo("system");
        assertThat(built.messages().stream().map(LlmMessage::content))
            .anyMatch(content -> content.contains("[older messages truncated]"));
        assertThat(built.messages().getLast().content()).isEqualTo("current question");
        assertThat(built.estimatedContextTokens()).isPositive();
    }
}

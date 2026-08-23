package com.suilearn.api.agent.loop;

import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.context.ContextBuilder;
import com.suilearn.api.agent.context.ContextBuildResult;
import com.suilearn.api.agent.context.PromptBlockAssembler;
import com.suilearn.api.agent.context.SessionMessageHistory;
import com.suilearn.api.agent.context.TokenEstimator;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import com.suilearn.api.agent.llm.LlmUsage;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.TurnContext;
import com.suilearn.api.agent.runtime.TurnEventSink;
import com.suilearn.api.agent.runtime.TurnStatus;
import com.suilearn.api.agent.runtime.ToolRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class AgentLoop {
    private static final String PROMPT_RESOURCE = "agents/agent-loop/v1/system.md";
    private static final int MAX_EMPTY_ANSWER_NUDGES = 2;

    private final LlmClient client;
    private final ToolDispatcher dispatcher;
    private final ToolRegistry tools;
    private final AgentConfigurationProperties properties;
    private final Clock clock;
    private final String model;
    private final ContextBuilder contextBuilder;
    private final SessionMessageHistory history;

    public AgentLoop(LlmClient client, ToolDispatcher dispatcher, ToolRegistry tools,
                     AgentConfigurationProperties properties, Clock clock, String model) {
        this(client, dispatcher, tools, properties, clock, model,
            new ContextBuilder(TokenEstimator.conservativeCharacters(),
                new PromptBlockAssembler(TokenEstimator.conservativeCharacters()),
                properties.contextMaxTokens()), null);
    }

    public AgentLoop(LlmClient client, ToolDispatcher dispatcher, ToolRegistry tools,
                     AgentConfigurationProperties properties, Clock clock, String model,
                     ContextBuilder contextBuilder, SessionMessageHistory history) {
        this.client = client;
        this.dispatcher = dispatcher;
        this.tools = tools;
        this.properties = properties;
        this.clock = clock;
        this.model = model == null || model.isBlank() ? "suilearn-default" : model;
        this.contextBuilder = contextBuilder;
        this.history = history;
    }

    public LoopResult run(TurnContext context, CapabilityManifest manifest, TurnEventSink events) {
        ContextBuildResult built = contextBuilder.build(context, manifest,
            history == null ? List.of() : history.recent(context.sessionId(), context.turnId()));
        var messages = new ArrayList<>(built.messages());
        events.publish(EventType.PROGRESS, context.capability(), "context",
            "Context budget report", Map.of("estimatedContextTokens", built.estimatedContextTokens(),
                "trimmedMessages", built.trimmedMessages()));
        var toolSchemas = tools.openAiSchemas(manifest);
        var deadline = clock.instant().plus(properties.runTimeout());
        int toolCalls = 0;
        int emptyAnswers = 0;
        LlmUsage usage = LlmUsage.none();

        for (int step = 1; step <= properties.maxSteps(); step++) {
            if (events.bus().isTerminal()) {
                return new LoopResult(LoopResult.Status.FAILED, "", toolCalls, usage);
            }
            if (!clock.instant().isBefore(deadline)) {
                return finishExhausted(events, context.capability(), step, usage);
            }
            events.publish(EventType.THINKING, context.capability(), "loop",
                "Agent loop step " + step, Map.of("step", step));
            LlmRequest request = new LlmRequest(model, List.copyOf(messages), toolSchemas, 0.2, null);
            var response = client.chat(request);
            usage = new LlmUsage(usage.promptTokens() + response.usage().promptTokens(),
                usage.completionTokens() + response.usage().completionTokens());

            if (!response.toolCalls().isEmpty()) {
                if (toolCalls + response.toolCalls().size() > properties.maxToolCalls()) {
                    return finishExhausted(events, context.capability(), step, usage);
                }
                messages.add(LlmMessage.assistant(response.content(), response.toolCalls()));
                List<ToolExecution> executions;
                try {
                    executions = dispatcher.dispatch(context, manifest, response.toolCalls(), events);
                } catch (AskUserPauseException pause) {
                    try {
                        var reply = events.pauseForUser(pause.payload().questionId(), pause.payload().prompt(),
                            pause.payload().multiSelect(), properties.runTimeout());
                        messages.add(LlmMessage.tool("pause_for_user", reply.text()));
                        toolCalls++;
                        continue;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, context.capability(), "wait_for_input",
                            "Turn interrupted while waiting for user input.", Map.of("code", "AGENT_TURN_INTERRUPTED"));
                        return new LoopResult(LoopResult.Status.FAILED, "", toolCalls, usage);
                    } catch (TimeoutException timeout) {
                        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, context.capability(), "wait_for_input",
                            "Turn timed out while waiting for user input.", Map.of("code", "AGENT_TURN_TIMEOUT"));
                        return new LoopResult(LoopResult.Status.FAILED, "", toolCalls, usage);
                    }
                }
                for (ToolExecution execution : executions) {
                    messages.add(LlmMessage.tool(execution.call().id(),
                        execution.result().content()));
                }
                toolCalls += executions.size();
                continue;
            }

            String content = response.content() == null ? "" : response.content().strip();
            if (content.isBlank()) {
                emptyAnswers++;
                if (emptyAnswers > MAX_EMPTY_ANSWER_NUDGES) {
                    events.publish(EventType.ERROR, context.capability(), "loop",
                        "Model returned empty answers after nudges.", Map.of("code", "INVALID_MODEL_OUTPUT"));
                    events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, context.capability(), "loop",
                        "Model returned empty answers.", Map.of("code", "INVALID_MODEL_OUTPUT"));
                    return new LoopResult(LoopResult.Status.INVALID_MODEL_OUTPUT, "", toolCalls, usage);
                }
                messages.add(LlmMessage.user("Your previous response was empty. Answer the learner's question or call a tool."));
                continue;
            }

            events.publish(EventType.RESULT, context.capability(), "loop", content,
                Map.of("toolCalls", toolCalls, "promptTokens", usage.promptTokens(),
                    "completionTokens", usage.completionTokens(),
                    "estimatedContextTokens", built.estimatedContextTokens(),
                    "actualPromptTokens", usage.promptTokens()));
            events.publishTerminal(EventType.DONE, TurnStatus.COMPLETED, context.capability(), "loop",
                content, Map.of("toolCalls", toolCalls));
            return new LoopResult(LoopResult.Status.COMPLETED, content, toolCalls, usage);
        }

        return finishExhausted(events, context.capability(), properties.maxSteps(), usage);
    }

    private LoopResult finishExhausted(TurnEventSink events, String source, int step, LlmUsage usage) {
        events.publish(EventType.ERROR, source, "loop", "Loop budget exhausted.",
            Map.of("code", "BUDGET_EXHAUSTED", "step", step));
        events.publishTerminal(EventType.FAILED, TurnStatus.FAILED, source, "loop",
            "Agent loop budget exhausted.", Map.of("code", "BUDGET_EXHAUSTED", "step", step));
        return new LoopResult(LoopResult.Status.BUDGET_EXHAUSTED, "", step, usage);
    }

}

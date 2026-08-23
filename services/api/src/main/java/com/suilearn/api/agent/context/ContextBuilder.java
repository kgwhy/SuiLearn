package com.suilearn.api.agent.context;

import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.ArrayList;
import java.util.List;

public final class ContextBuilder {
    private final TokenEstimator estimator;
    private final PromptBlockAssembler prompts;
    private final int contextMaxTokens;

    public ContextBuilder(TokenEstimator estimator, PromptBlockAssembler prompts, int contextMaxTokens) {
        if (contextMaxTokens < 1) {
            throw new IllegalArgumentException("contextMaxTokens must be positive");
        }
        this.estimator = estimator;
        this.prompts = prompts;
        this.contextMaxTokens = contextMaxTokens;
    }

    public ContextBuildResult build(TurnContext context, CapabilityManifest manifest,
                                    List<LlmMessage> history) {
        return build(context, manifest, history, "");
    }

    public ContextBuildResult build(TurnContext context, CapabilityManifest manifest,
                                    List<LlmMessage> history, String sessionSummary) {
        var memoryBlocks = sessionSummary == null || sessionSummary.isBlank()
            ? List.<PromptBlock>of()
            : List.of(new PromptBlock("session_summary", sessionSummary,
                estimator.estimate(sessionSummary)));
        var prompt = prompts.assemble(manifest, memoryBlocks);
        var retained = new ArrayList<LlmMessage>();
        var historyBudget = Math.max(1, (int) (contextMaxTokens * 0.35d));
        int historyTokens = 0;
        boolean truncated = false;
        int trimmed = 0;

        // First pass: drop old tool messages while over budget.
        var filtered = new ArrayList<LlmMessage>(history == null ? List.of() : history);
        while (estimated(history, filtered) > historyBudget && removeOldestTool(filtered)) {
            trimmed++;
            truncated = true;
        }
        // If still over budget, drop oldest messages regardless of role.
        while (estimated(history, filtered) > historyBudget && !filtered.isEmpty()) {
            filtered.removeFirst();
            trimmed++;
            truncated = true;
        }
        if (truncated) {
            retained.add(LlmMessage.user("[older messages truncated]"));
        }
        retained.addAll(filtered);
        retained.add(LlmMessage.user(context.userMessage()));
        retained.addFirst(LlmMessage.system(prompt.content()));
        int systemTokens = prompt.estimatedTokens();
        int total = systemTokens + retained.stream().mapToInt(message -> estimator.estimate(message.content())).sum();
        return new ContextBuildResult(prompt.content(), List.copyOf(retained), total, trimmed);
    }

    private boolean removeOldestTool(List<LlmMessage> messages) {
        for (int index = 0; index < messages.size(); index++) {
            if ("tool".equals(messages.get(index).role())) {
                messages.remove(index);
                return true;
            }
        }
        return false;
    }

    private int estimated(List<LlmMessage> original, List<LlmMessage> current) {
        int total = 0;
        for (LlmMessage message : current) {
            total += estimator.estimate(message.content());
        }
        return total;
    }
}

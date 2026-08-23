package com.suilearn.api.agent.context;

import com.suilearn.api.agent.capability.CapabilityManifest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

public final class PromptBlockAssembler {
    public static final String POLICY_RESOURCE = "agents/agent-loop/v1/system.md";

    private final TokenEstimator estimator;

    public PromptBlockAssembler(TokenEstimator estimator) {
        this.estimator = estimator;
    }

    public Assembled assemble(CapabilityManifest manifest, List<PromptBlock> memoryBlocks) {
        var blocks = new java.util.ArrayList<PromptBlock>();
        blocks.add(block("general", "You are SuiLearn's bounded study agent."));
        blocks.add(block("policy", policy()));
        blocks.add(block("capability", "Capability: " + manifest.name()));
        blocks.add(block("memory", memoryBlocks == null || memoryBlocks.isEmpty()
            ? "No long-term memory injected for this turn." : join(memoryBlocks)));
        blocks.add(block("tools", "Available tools: " + String.join(", ", manifest.ownedTools())));
        blocks.add(block("skills", "No extra skills enabled."));
        String content = blocks.stream().map(PromptBlock::content).reduce("", (left, right) ->
            left.isBlank() ? right : left + "\n\n" + right);
        return new Assembled(content, List.copyOf(blocks), estimator.estimate(content));
    }

    private PromptBlock block(String name, String content) {
        return new PromptBlock(name, content, estimator.estimate(content));
    }

    private String policy() {
        try {
            return new String(new ClassPathResource(POLICY_RESOURCE).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("agent loop policy resource is unavailable", exception);
        }
    }

    private String join(List<PromptBlock> blocks) {
        return blocks.stream().map(PromptBlock::content).reduce("", (left, right) ->
            left.isBlank() ? right : left + "\n" + right);
    }

    public record Assembled(String content, List<PromptBlock> blocks, int estimatedTokens) {
        public Assembled {
            content = content == null ? "" : content;
            blocks = List.copyOf(blocks);
            if (estimatedTokens < 1) {
                throw new IllegalArgumentException("estimatedTokens must be positive");
            }
        }
    }
}

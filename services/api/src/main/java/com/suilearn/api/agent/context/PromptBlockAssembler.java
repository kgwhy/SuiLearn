package com.suilearn.api.agent.context;

import com.suilearn.api.agent.capability.CapabilityManifest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

public final class PromptBlockAssembler {
    public static final String POLICY_RESOURCE = "agents/agent-loop/v1/system.md";
    public static final String RAG_QA_POLICY_RESOURCE = "agents/agent-loop/v1/rag-qa.md";
    public static final String QUESTION_GENERATION_POLICY_RESOURCE = "agents/agent-loop/v1/question-generation.md";

    private final TokenEstimator estimator;

    public PromptBlockAssembler(TokenEstimator estimator) {
        this.estimator = estimator;
    }

    public Assembled assemble(CapabilityManifest manifest, List<PromptBlock> memoryBlocks) {
        var blocks = new java.util.ArrayList<PromptBlock>();
        blocks.add(block("general", generalFor(manifest.name())));
        blocks.add(block("policy", policy(manifest.name())));
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

    private String generalFor(String capability) {
        return switch (capability) {
            case com.suilearn.api.agent.capability.BuiltinCapabilities.RAG_QA ->
                "You are SuiLearn's bounded RAG question-answering agent.";
            case com.suilearn.api.agent.capability.BuiltinCapabilities.QUESTION_GENERATION ->
                "You are SuiLearn's bounded practice question generator.";
            default -> "You are SuiLearn's bounded study agent.";
        };
    }

    private String policy(String capability) {
        String resource = switch (capability) {
            case com.suilearn.api.agent.capability.BuiltinCapabilities.RAG_QA -> RAG_QA_POLICY_RESOURCE;
            case com.suilearn.api.agent.capability.BuiltinCapabilities.QUESTION_GENERATION ->
                QUESTION_GENERATION_POLICY_RESOURCE;
            default -> POLICY_RESOURCE;
        };
        try {
            return new String(new ClassPathResource(resource).getInputStream().readAllBytes(),
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

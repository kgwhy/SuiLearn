package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
import com.suilearn.api.agent.learner.LearnerProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBlockAssemblerTest {
    @Test
    void blocksAreStableAndOrdered() {
        var assembler = new PromptBlockAssembler(TokenEstimator.conservativeCharacters());
        var manifest = BuiltinCapabilities.studyAgent().manifest();

        var first = assembler.assemble(manifest, List.of());
        var second = assembler.assemble(manifest, List.of());

        assertThat(first.content()).isEqualTo(second.content());
        assertThat(first.estimatedTokens()).isEqualTo(second.estimatedTokens());
        assertThat(first.blocks()).extracting(PromptBlock::name)
            .containsExactly("general", "policy", "capability", "memory", "tools", "skills");
        assertThat(first.content()).contains("study_agent", "search_knowledge", "ask_user");
    }

    @Test
    void learnerProfileAddsPersonaAndSkillsBlocks() {
        var assembler = new PromptBlockAssembler(TokenEstimator.conservativeCharacters());

        var assembled = assembler.assemble(BuiltinCapabilities.studyAgent().manifest(), List.of(),
            new LearnerProfile("learner-a", "visual learner", List.of("Java", "Spring")));

        assertThat(assembled.blocks()).extracting(PromptBlock::name)
            .contains("persona", "skills");
        assertThat(assembled.content()).contains("Learner persona: visual learner", "Learner skills: Java, Spring");
    }

    @Test
    void capabilitySpecificPoliciesAreSelected() {
        var assembler = new PromptBlockAssembler(TokenEstimator.conservativeCharacters());

        var rag = assembler.assemble(BuiltinCapabilities.ragQa().manifest(), List.of());
        var generation = assembler.assemble(BuiltinCapabilities.questionGeneration().manifest(), List.of());

        assertThat(rag.content()).contains("RAG question-answering agent", "rag_qa", "search_knowledge")
            .doesNotContain("generate_practice");
        assertThat(generation.content()).contains("practice question generator", "question_generation",
            "generate_practice").doesNotContain("search_knowledge");
    }
}

package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.capability.BuiltinCapabilities;
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
}

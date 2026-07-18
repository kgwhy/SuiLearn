package com.suilearn.api.agent.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import com.suilearn.api.agent.infrastructure.springai.SpringAiPromptTemplateRenderer;
import org.junit.jupiter.api.Test;

class PromptRegistryTest {
    private final PromptRegistry registry = new PromptRegistry(new SpringAiPromptTemplateRenderer());

    @Test
    void exposesOnlyFixedNameAndVersionAllowlistWithStableHashes() {
        assertThat(registry.allowlist()).containsExactlyInAnyOrder(
            new PromptKey("supervisor", "v1"),
            new PromptKey("knowledge-research", "v1"),
            new PromptKey("practice-coach", "v1"),
            new PromptKey("memory-extraction", "v1"));

        for (PromptKey key : registry.allowlist()) {
            PromptDocument first = registry.load(key.name(), key.version());
            PromptDocument second = registry.load(key.name(), key.version());
            assertThat(first.sha256()).matches("[0-9a-f]{64}").isEqualTo(second.sha256());
            assertThat(first.content()).contains("# Role", "# Goal", "# Immutable constraints",
                "# Stop and failure policy", "# Evidence policy", "# Output schema");
        }
    }

    @Test
    void rejectsUnknownNameVersionAndArbitraryPathWithoutReadingIt() {
        assertThatThrownBy(() -> registry.load("supervisor", "v2"))
            .isInstanceOf(PromptRegistry.UnknownPromptException.class);
        assertThatThrownBy(() -> registry.load("../../application", "v1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("invalid prompt name");
    }

    @Test
    void requiresTheRegisteredTypedVariablesAndRendersEveryVariableExactly() {
        var rendered = registry.render("supervisor", "v1",
            new PromptVariables.Supervisor("learn-react", "kb:1", "untrusted-context"));

        assertThat(rendered.name()).isEqualTo("supervisor");
        assertThat(rendered.version()).isEqualTo("v1");
        assertThat(rendered.content()).contains("learn-react", "kb:1", "untrusted-context")
            .doesNotContain("{{task}}", "{{scope}}", "{{context}}");
        assertThatThrownBy(() -> registry.render("supervisor", "v1",
            new PromptVariables.MemoryExtraction("outcome", "ref", "schema")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("typed variables do not match registered prompt");
    }
}

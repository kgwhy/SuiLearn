package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.rag.parsing.ParseEngineRegistry;
import com.suilearn.api.rag.parsing.TextParseEngine;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParseEngineRegistryTest {
    @Test
    void routesTextAndRejectsUnknown() {
        var registry = new ParseEngineRegistry(List.of(new TextParseEngine()));
        var parsed = registry.parse("text/markdown", "# Title".getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.text()).isEqualTo("# Title");
        assertThat(registry.engines()).hasSize(1);
        assertThatThrownBy(() -> registry.parse("application/pdf", new byte[]{1}))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

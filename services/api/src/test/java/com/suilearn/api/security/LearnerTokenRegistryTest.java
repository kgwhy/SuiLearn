package com.suilearn.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearnerTokenRegistryTest {
    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void parsesJsonAndResolvesToken() {
        var registry = LearnerTokenRegistry.fromJson(
            "[{\"token\":\"token-learner-a\",\"learnerId\":\"learner-a\"}]", mapper);

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.resolve("token-learner-a")).contains(new LearnerPrincipal("learner-a"));
        assertThat(registry.resolve("missing-token")).isEmpty();
    }

    @Test
    void blankJsonCreatesEmptyRegistry() {
        var registry = LearnerTokenRegistry.fromJson("", mapper);
        assertThat(registry.size()).isZero();
    }

    @Test
    void rejectsDuplicateOrShortTokens() {
        assertThatThrownBy(() -> LearnerTokenRegistry.fromJson(
            "[{\"token\":\"token-one\",\"learnerId\":\"a\"},{\"token\":\"token-one\",\"learnerId\":\"b\"}]", mapper))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LearnerTokenRegistry(List.of(
            new LearnerTokenRegistry.TokenBinding("short", "a"))))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

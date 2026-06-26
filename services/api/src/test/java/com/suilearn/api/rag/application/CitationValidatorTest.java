package com.suilearn.api.rag.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.ai.AiProvider.GeneratedAnswer;
import com.suilearn.api.ai.AiProvider.GeneratedStatement;
import java.util.List;
import org.junit.jupiter.api.Test;

class CitationValidatorTest {
    private final CitationValidator validator = new CitationValidator();

    @Test
    void rejectsCertainAnswerWithoutCitation() {
        var result = validator.validate(new GeneratedAnswer("HashMap uses buckets.", false), 2);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("引用");
    }

    @Test
    void rejectsCitationOutsideEvidenceRange() {
        var result = validator.validate(new GeneratedAnswer("HashMap uses buckets [3].", false), 2);

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("不存在");
    }

    @Test
    void acceptsStatementCitationsWithinEvidenceRange() {
        var result = validator.validate(
            new GeneratedAnswer(
                "HashMap uses buckets [1].",
                false,
                List.of(new GeneratedStatement("HashMap uses buckets.", List.of(1)))
            ),
            1
        );

        assertThat(result.valid()).isTrue();
    }
}

package com.suilearn.api.agent.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StructuredAgentOutputTest {
    @Test
    void validatesRequiredFieldsLengthsActionsAndCitationScope() {
        var validator = new StructuredAgentOutputValidator(Set.of("source-1"), 12);

        assertThat(validator.validate(new StructuredAgentOutput(
            StructuredAgentOutput.Action.ANSWER, "grounded", List.of("source-1"))).valid()).isTrue();
        assertThat(validator.validate(new StructuredAgentOutput(
            StructuredAgentOutput.Action.ANSWER, "answer that is too long", List.of("outside"))).reasonCodes())
            .containsExactly("ANSWER_TOO_LONG", "CITATION_OUT_OF_SCOPE");
        assertThat(validator.validate(new StructuredAgentOutput(
            StructuredAgentOutput.Action.UNCERTAIN, "uncertain", List.of("source-1"))).reasonCodes())
            .containsExactly("UNCERTAIN_WITH_CITATION");
    }

    @Test
    void repairsAtMostOnceAndRecordsTheBoundedAction() {
        AtomicInteger repairs = new AtomicInteger();
        var processor = new StructuredOutputProcessor<>(
            this::decodeFixture,
            new StructuredAgentOutputValidator(Set.of("source-1"), 50),
            (invalidOutput, reasonCodes) -> {
                repairs.incrementAndGet();
                assertThat(invalidOutput).isEqualTo("missing-citation");
                assertThat(reasonCodes).containsExactly("ANSWER_WITHOUT_CITATION");
                return "valid";
            });

        StructuredOutputProcessor.Result<StructuredAgentOutput> result = processor.process("missing-citation");

        assertThat(result.value().answer()).isEqualTo("grounded answer");
        assertThat(result.repairCount()).isEqualTo(1);
        assertThat(repairs).hasValue(1);
    }

    @Test
    void rejectsASecondInvalidOutputWithoutLeakingEitherModelBody() {
        String firstSecret = "first-model-secret";
        String secondSecret = "second-model-secret";
        AtomicInteger repairs = new AtomicInteger();
        var processor = new StructuredOutputProcessor<StructuredAgentOutput>(
            raw -> { throw new IllegalArgumentException(raw); },
            output -> ValidationResult.success(),
            (invalidOutput, reasonCodes) -> {
                repairs.incrementAndGet();
                assertThat(invalidOutput).isEqualTo(firstSecret);
                return secondSecret;
            });

        assertThatThrownBy(() -> processor.process(firstSecret))
            .isInstanceOf(StructuredOutputProcessor.InvalidModelOutputException.class)
            .hasMessage("INVALID_MODEL_OUTPUT")
            .hasMessageNotContaining(firstSecret)
            .hasMessageNotContaining(secondSecret)
            .satisfies(error -> {
                var invalid = (StructuredOutputProcessor.InvalidModelOutputException) error;
                assertThat(invalid.repairCount()).isEqualTo(1);
                assertThat(invalid.reasonCodes()).containsExactly("DECODE_FAILED");
            });
        assertThat(repairs).hasValue(1);
    }

    @Test
    void removesRepairFailureCauseThatCouldContainModelContent() {
        String secret = "repair-provider-secret";
        var processor = new StructuredOutputProcessor<StructuredAgentOutput>(
            raw -> { throw new IllegalArgumentException("invalid"); },
            output -> ValidationResult.success(),
            (invalidOutput, reasonCodes) -> { throw new IllegalStateException(secret); });

        assertThatThrownBy(() -> processor.process("invalid-output"))
            .isInstanceOf(StructuredOutputProcessor.InvalidModelOutputException.class)
            .hasMessage("INVALID_MODEL_OUTPUT")
            .hasMessageNotContaining(secret)
            .hasNoCause();
    }

    private StructuredAgentOutput decodeFixture(String raw) {
        return switch (raw) {
            case "missing-citation" -> new StructuredAgentOutput(
                StructuredAgentOutput.Action.ANSWER, "grounded answer", List.of());
            case "valid" -> new StructuredAgentOutput(
                StructuredAgentOutput.Action.ANSWER, "grounded answer", List.of("source-1"));
            default -> throw new IllegalArgumentException("invalid fixture");
        };
    }
}

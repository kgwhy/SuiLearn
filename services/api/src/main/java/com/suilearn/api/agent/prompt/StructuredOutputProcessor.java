package com.suilearn.api.agent.prompt;

import java.util.List;
import java.util.Objects;

public final class StructuredOutputProcessor<T> {
    private final OutputDecoder<T> decoder;
    private final OutputValidator<T> validator;
    private final OutputRepair repair;

    public StructuredOutputProcessor(OutputDecoder<T> decoder, OutputValidator<T> validator, OutputRepair repair) {
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.repair = Objects.requireNonNull(repair, "repair");
    }

    public Result<T> process(String rawOutput) {
        Attempt<T> first = attempt(rawOutput);
        if (first.valid()) {
            return new Result<>(first.value(), 0, List.of());
        }

        String repaired;
        try {
            repaired = repair.repair(rawOutput, first.reasonCodes());
        } catch (RuntimeException exception) {
            if ("BUDGET_EXHAUSTED".equals(exception.getMessage())) {
                throw exception;
            }
            throw new InvalidModelOutputException(first.reasonCodes(), 1);
        }
        Attempt<T> second = attempt(repaired);
        if (second.valid()) {
            return new Result<>(second.value(), 1, first.reasonCodes());
        }
        throw new InvalidModelOutputException(second.reasonCodes(), 1);
    }

    private Attempt<T> attempt(String rawOutput) {
        try {
            T value = decoder.decode(rawOutput);
            ValidationResult validation = validator.validate(value);
            return validation.valid()
                ? new Attempt<>(value, List.of())
                : new Attempt<>(null, validation.reasonCodes());
        } catch (RuntimeException exception) {
            return new Attempt<>(null, List.of("DECODE_FAILED"));
        }
    }

    @FunctionalInterface
    public interface OutputDecoder<T> {
        T decode(String rawOutput);
    }

    @FunctionalInterface
    public interface OutputValidator<T> {
        ValidationResult validate(T output);
    }

    @FunctionalInterface
    public interface OutputRepair {
        String repair(String invalidOutput, List<String> reasonCodes);
    }

    public record Result<T>(T value, int repairCount, List<String> initialReasonCodes) {
        public Result {
            Objects.requireNonNull(value, "value");
            initialReasonCodes = List.copyOf(initialReasonCodes);
            if (repairCount < 0 || repairCount > 1) {
                throw new IllegalArgumentException("repairCount must be zero or one");
            }
        }
    }

    private record Attempt<T>(T value, List<String> reasonCodes) {
        boolean valid() {
            return value != null && reasonCodes.isEmpty();
        }
    }

    public static final class InvalidModelOutputException extends IllegalArgumentException {
        private final List<String> reasonCodes;
        private final int repairCount;

        private InvalidModelOutputException(List<String> reasonCodes, int repairCount) {
            super("INVALID_MODEL_OUTPUT");
            this.reasonCodes = List.copyOf(reasonCodes);
            this.repairCount = repairCount;
        }

        public List<String> reasonCodes() {
            return reasonCodes;
        }

        public int repairCount() {
            return repairCount;
        }
    }
}

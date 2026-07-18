package com.suilearn.api.agent.prompt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class StructuredAgentOutputValidator
    implements StructuredOutputProcessor.OutputValidator<StructuredAgentOutput> {
    private final Set<String> allowedCitationRefs;
    private final int maximumAnswerLength;

    public StructuredAgentOutputValidator(Set<String> allowedCitationRefs, int maximumAnswerLength) {
        this.allowedCitationRefs = Set.copyOf(allowedCitationRefs == null ? Set.of() : allowedCitationRefs);
        if (maximumAnswerLength < 1) {
            throw new IllegalArgumentException("maximumAnswerLength must be positive");
        }
        this.maximumAnswerLength = maximumAnswerLength;
    }

    @Override
    public ValidationResult validate(StructuredAgentOutput output) {
        var reasons = new ArrayList<String>();
        if (output == null || output.action() == null) {
            reasons.add("ACTION_REQUIRED");
        }
        if (output == null || output.answer() == null || output.answer().isBlank()) {
            reasons.add("ANSWER_REQUIRED");
        } else if (output.answer().codePointCount(0, output.answer().length()) > maximumAnswerLength) {
            reasons.add("ANSWER_TOO_LONG");
        }
        if (output != null) {
            if (new HashSet<>(output.citations()).size() != output.citations().size()) {
                reasons.add("DUPLICATE_CITATION");
            }
            if (!allowedCitationRefs.containsAll(output.citations())) {
                reasons.add("CITATION_OUT_OF_SCOPE");
            }
            if (output.action() == StructuredAgentOutput.Action.UNCERTAIN && !output.citations().isEmpty()) {
                reasons.add("UNCERTAIN_WITH_CITATION");
            }
            if (output.action() == StructuredAgentOutput.Action.ANSWER && output.citations().isEmpty()) {
                reasons.add("ANSWER_WITHOUT_CITATION");
            }
        }
        return reasons.isEmpty()
            ? ValidationResult.success()
            : new ValidationResult(false, reasons);
    }
}

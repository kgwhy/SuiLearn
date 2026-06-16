package com.suilearn.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SubmitAnswerRequest(
    @NotNull
    String questionId,
    @NotEmpty
    List<String> userAnswer,
    @NotNull
    Boolean correct,
    long durationMs
) {
}

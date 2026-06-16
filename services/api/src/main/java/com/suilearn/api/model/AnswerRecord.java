package com.suilearn.api.model;

import java.time.Instant;
import java.util.List;

public record AnswerRecord(
    String id,
    String knowledgeBaseId,
    String questionId,
    List<String> userAnswer,
    boolean correct,
    long durationMs,
    Instant answeredAt
) {
}

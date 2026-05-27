package com.suilearn.api.model;

import java.util.List;

public record KnowledgeBaseStatistics(
    String knowledgeBaseId,
    int questionCount,
    int answeredQuestionCount,
    int answerCount,
    Double correctRate,
    int wrongQuestionCount,
    List<String> weakKnowledgePointIds
) {
}

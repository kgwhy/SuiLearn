package com.suilearn.api.model;

import java.util.List;

public record KnowledgePointExtractionResult(
    String taskId,
    TaskStatus task,
    List<KnowledgePoint> knowledgePoints
) {
}

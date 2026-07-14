package com.suilearn.api.task.application;

import java.util.List;

public final class MessagingTopology {
    private static final List<String> QUEUES = List.of(
        "document.processing", "knowledge-point.generation", "question.generation"
    );

    private MessagingTopology() { }

    public static List<String> queueNames() { return QUEUES; }

    public static List<String> deadLetterQueueNames() {
        return QUEUES.stream().map(name -> name + ".dlq").toList();
    }
}

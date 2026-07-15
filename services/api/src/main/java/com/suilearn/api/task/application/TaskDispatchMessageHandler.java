package com.suilearn.api.task.application;

import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.generation.application.KnowledgePointQuestionGenerationService;
import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** Establishes the persisted task dispatch transition; stage-specific work is attached by Task 3.4. */
@Component
final class TaskDispatchMessageHandler implements ProcessingMessageHandler {
    static final String TASK_ID_HEADER = "x-suilearn-task-id";
    static final String STAGE_HEADER = "x-suilearn-stage";
    private final TaskService tasks;
    private final MaterialImportService materialImports;
    private final KnowledgePointService knowledgePoints;
    private final KnowledgePointQuestionGenerationService questions;
    private final ObjectMapper objectMapper;

    TaskDispatchMessageHandler(TaskService tasks, MaterialImportService materialImports) {
        this(tasks, materialImports, null, null, null);
    }

    @Autowired
    TaskDispatchMessageHandler(
        TaskService tasks, MaterialImportService materialImports, KnowledgePointService knowledgePoints,
        KnowledgePointQuestionGenerationService questions, ObjectMapper objectMapper
    ) {
        this.tasks = tasks;
        this.materialImports = materialImports;
        this.knowledgePoints = knowledgePoints;
        this.questions = questions; this.objectMapper = objectMapper;
    }

    @Override
    public void handle(Message message) {
        String taskId = stringHeader(message, TASK_ID_HEADER);
        String stage = stringHeader(message, STAGE_HEADER);
        var task = tasks.getTaskStatus(taskId);
        if (isMaterialProcessing(task.kind()) && task.status() == TaskLifecycleStatus.QUEUED) {
            materialImports.consumeQueuedMaterialImport(task.materialId(), task.id());
            return;
        }
        if (task.kind() == TaskKind.KNOWLEDGE_POINT_EXTRACTION && task.status() == TaskLifecycleStatus.QUEUED) {
            if (knowledgePoints == null) throw new IllegalStateException("Knowledge point generation dispatcher is unavailable");
            String revisionId = submittedRevisionId(message);
            if (revisionId == null) knowledgePoints.consumeGeneration(task.id(), task.materialId());
            else knowledgePoints.consumeGeneration(task.id(), task.materialId(), revisionId);
            return;
        }
        if (task.kind() == TaskKind.QUESTION_GENERATION && task.status() == TaskLifecycleStatus.QUEUED) {
            if (questions == null || objectMapper == null) throw new IllegalStateException("Question generation dispatcher is unavailable");
            try { questions.consume(task.id(), objectMapper.readValue(message.getBody(), KnowledgePointQuestionGenerationService.Payload.class)); }
            catch (java.io.IOException exception) { throw new IllegalArgumentException("Invalid question generation payload", exception); }
            return;
        }
        if (task.status() == TaskLifecycleStatus.QUEUED) {
            tasks.startTask(task, stage);
        }
    }

    private boolean isMaterialProcessing(TaskKind kind) {
        return kind == TaskKind.MATERIAL_IMPORT || kind == TaskKind.MATERIAL_REPROCESS;
    }

    private String stringHeader(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required processing message header: " + name);
        }
        return text;
    }

    private String submittedRevisionId(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        return payload.startsWith("revision:") ? payload.substring("revision:".length()) : null;
    }
}

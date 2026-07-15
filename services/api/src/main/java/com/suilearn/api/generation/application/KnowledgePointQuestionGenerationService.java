package com.suilearn.api.generation.application;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.dto.GenerateKnowledgePointInterviewQuestionsRequest;
import com.suilearn.api.generation.domain.InterviewQuestionDifficulty;
import com.suilearn.api.generation.infrastructure.GeneratedContentStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePointQuestionGenerationService {
    private final AiProvider aiProvider;
    private final Clock clock;
    private final GeneratedContentStore generatedContents;
    private final KnowledgePointStore knowledgePoints;
    private final TaskService taskService;
    private final TaskOutboxSubmissionService submissions;
    private final ObjectMapper objectMapper;

    public KnowledgePointQuestionGenerationService(
        AiProvider aiProvider, KnowledgePointStore knowledgePoints, GeneratedContentStore generatedContents,
        TaskService taskService, TaskOutboxSubmissionService submissions, ObjectMapper objectMapper, Clock clock
    ) {
        this.aiProvider = aiProvider;
        this.clock = clock;
        this.generatedContents = generatedContents;
        this.knowledgePoints = knowledgePoints;
        this.taskService = taskService;
        this.submissions = submissions;
        this.objectMapper = objectMapper;
    }

    public Submission submit(String knowledgeBaseId, String knowledgePointId, GenerateKnowledgePointInterviewQuestionsRequest request) {
        var point = requireEligiblePoint(knowledgeBaseId, knowledgePointId);
        var quantity = quantity(request.quantity());
        var difficulty = request.difficulty() == null ? InterviewQuestionDifficulty.MEDIUM : request.difficulty();
        var type = request.questionType() == null ? QuestionType.SHORT_ANSWER : request.questionType();
        var task = submissions.submit(TaskKind.QUESTION_GENERATION, knowledgeBaseId, point.sourceMaterialId(), AiProviderType.OPENAI_COMPATIBLE,
            "openai-compatible-chat", "QUEUED", "GENERATING_QUESTIONS", point.id(), payload(knowledgeBaseId, point.id(), request));
        return new Submission(task.id(), List.of(), quantity, difficulty, type);
    }

    /** HTTP callers supply ownership through the route, not through client-controlled JSON. */
    public Submission submit(String knowledgePointId, GenerateKnowledgePointInterviewQuestionsRequest request) {
        var point = knowledgePoints.find(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
        return submit(point.knowledgeBaseId(), knowledgePointId, request);
    }

    public Submission consume(String taskId, Payload payload) {
        var point = requireEligiblePoint(payload.knowledgeBaseId(), payload.knowledgePointId());
        var quantity = quantity(payload.quantity()); var difficulty = payload.difficulty() == null ? InterviewQuestionDifficulty.MEDIUM : payload.difficulty();
        var type = payload.questionType() == null ? QuestionType.SHORT_ANSWER : payload.questionType();
        var task = taskService.startTask(taskService.getTaskStatus(taskId), "GENERATING");
        try {
            var drafts = java.util.stream.IntStream.range(0, quantity)
                .mapToObj(ignored -> createDraft(task.id(), point, difficulty, type)).toList();
            drafts.forEach(generatedContents::save);
            var finished = taskService.updateTask(task, TaskLifecycleStatus.SUCCEEDED, 100, "PENDING_REVIEW",
                new TaskResultRef("QUESTION_DRAFTS", task.id(), drafts.size()), null, null, point.sourceMaterialId(), null);
            return new Submission(finished.id(), drafts, quantity, difficulty, type);
        } catch (RuntimeException exception) {
            taskService.updateTask(task, TaskLifecycleStatus.FAILED, 100, "FAILED", null,
                "QUESTION_GENERATION_FAILED", safeError(exception), point.sourceMaterialId(), null);
            throw exception;
        }
    }

    public List<GeneratedQuestionDraft> listDrafts(String taskId) {
        return generatedContents.list().stream()
            .filter(draft -> taskId.equals(draft.generationTaskId()))
            .toList();
    }

    private String payload(
        String knowledgeBaseId, String knowledgePointId, GenerateKnowledgePointInterviewQuestionsRequest request
    ) {
        try { return objectMapper.writeValueAsString(new Payload("v1", knowledgeBaseId, knowledgePointId, request.quantity(), request.difficulty(), request.questionType())); }
        catch (Exception exception) { throw new IllegalStateException("Failed to serialize question generation payload", exception); }
    }

    /** Durable outbox payload; consumer-only execution keeps AI out of HTTP threads. */
    public record Payload(String version, String knowledgeBaseId, String knowledgePointId, Integer quantity,
                          InterviewQuestionDifficulty difficulty, QuestionType questionType) {}

    private GeneratedQuestionDraft createDraft(
        String taskId, KnowledgePoint point, InterviewQuestionDifficulty difficulty, QuestionType type
    ) {
        var generated = aiProvider.generateQuestion(new AiProvider.QuestionGenerationPrompt(
            point.knowledgeBaseId(), point.sourceRefs(), SourceType.KNOWLEDGE_POINT, point.id(), type,
            point.id(), point.title(), List.of(point.id()), null, difficulty
        ));
        var stem = required(generated.stem(), "question stem");
        var answer = generated.answer() == null || generated.answer().isEmpty() ? List.<String>of() : generated.answer();
        if (answer.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("AI returned blank answer");
        }
        var evidence = point.sourceRefs().getFirst();
        var now = clock.instant();
        return new GeneratedQuestionDraft(newId("gen"), point.knowledgeBaseId(), taskId, GeneratedContentStatus.PENDING_REVIEW,
            point.sourceRefs(), SourceType.KNOWLEDGE_POINT, point.id(), generated.questionType() == null ? type : generated.questionType(),
            point.id(), point.title(), List.of(point.id()), stem, generated.options() == null ? List.of() : generated.options(), answer,
            required(generated.explanation(), "question explanation"), null, null, now, now, point.id(), evidence.materialId(),
            evidence.revisionId(), evidence.excerpt());
    }

    private KnowledgePoint requireEligiblePoint(String knowledgeBaseId, String knowledgePointId) {
        var point = knowledgePoints.find(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
        if (!point.knowledgeBaseId().equals(knowledgeBaseId)) throw new IllegalArgumentException("Knowledge point is outside knowledge base");
        if (point.reviewStatus() != KnowledgePointReviewStatus.CONFIRMED) throw new IllegalStateException("Knowledge point must be CONFIRMED");
        if (point.legacy()) throw new IllegalStateException("Legacy knowledge point must be regenerated");
        if (point.sourceOutdated()) throw new IllegalStateException("Knowledge point source is outdated");
        if (point.sourceRefs() == null || point.sourceRefs().stream().noneMatch(this::isCurrentCitation)) {
            throw new IllegalStateException("Knowledge point requires a current versioned citation");
        }
        return point;
    }

    private boolean isCurrentCitation(SourceRef ref) {
        return ref != null && !ref.deleted() && ref.materialId() != null && ref.revisionId() != null
            && (ref.pageNumber() != null || (ref.blockId() != null && !ref.blockId().isBlank()));
    }

    private int quantity(Integer requested) {
        int value = requested == null ? 1 : requested;
        if (value < 1 || value > 10) throw new IllegalArgumentException("quantity must be between 1 and 10");
        return value;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException("AI returned blank " + field);
        return value;
    }

    private String safeError(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.substring(0, Math.min(160, message.length()));
    }

    private String newId(String prefix) { return prefix + "_" + UUID.randomUUID().toString().replace("-", ""); }

    public record Submission(
        String taskId, List<GeneratedQuestionDraft> drafts, int quantity, InterviewQuestionDifficulty difficulty, QuestionType questionType
    ) {
    }
}

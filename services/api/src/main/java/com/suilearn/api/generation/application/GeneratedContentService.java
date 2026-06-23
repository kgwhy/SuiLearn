package com.suilearn.api.generation.application;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.generation.infrastructure.AiNoteStore;
import com.suilearn.api.generation.infrastructure.GeneratedContentStore;
import com.suilearn.api.generation.infrastructure.QuestionStore;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GeneratedContentService {
    private final AiProvider aiProvider;
    private final AiNoteStore aiNotes;
    private final Clock clock;
    private final GeneratedContentStore generatedContents;
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final QuestionStore questions;
    private final SourceService sourceService;
    private final TaskService taskService;

    public GeneratedContentService(
        KnowledgeBaseStore knowledgeBases,
        KnowledgePointStore knowledgePoints,
        GeneratedContentStore generatedContents,
        QuestionStore questions,
        AiNoteStore aiNotes,
        AiProvider aiProvider,
        TaskService taskService,
        SourceService sourceService,
        Clock clock
    ) {
        this.aiProvider = aiProvider;
        this.aiNotes = aiNotes;
        this.clock = clock;
        this.generatedContents = generatedContents;
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.questions = questions;
        this.sourceService = sourceService;
        this.taskService = taskService;
    }

    public GeneratedQuestionDraft generateQuestion(GenerateQuestionRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var sourceRefs = sourceService.ensureUsable(sourceService.normalize(request.knowledgeBaseId(), request.sourceRefs()));
        var task = taskService.startTask(taskService.createTask(
            TaskKind.QUESTION_GENERATION,
            request.knowledgeBaseId(),
            sourceService.firstMaterialId(sourceRefs),
            aiProviderType(),
            chatModelName(),
            "GENERATING"
        ), "GENERATING");
        var now = clock.instant();
        var type = request.questionType() == null ? QuestionType.SINGLE_CHOICE : request.questionType();
        var draftKnowledgePointIds = requestedKnowledgePointIds(request.knowledgePointIds(), sourceRefs);
        var categoryId = valueOrDefault(request.categoryId(), defaultCategoryId(draftKnowledgePointIds));
        var categoryName = valueOrDefault(request.categoryName(), defaultCategoryName(categoryId));
        try {
            var generated = aiProvider.generateQuestion(new AiProvider.QuestionGenerationPrompt(
                request.knowledgeBaseId(),
                sourceRefs,
                request.sourceType() == null ? sourceRefs.get(0).type() : request.sourceType(),
                request.sourceId() == null ? sourceRefs.get(0).id() : request.sourceId(),
                type,
                categoryId,
                categoryName,
                draftKnowledgePointIds,
                request.prompt()
            ));
            var draft = new GeneratedQuestionDraft(
                newId("gen"),
                request.knowledgeBaseId(),
                task.id(),
                GeneratedContentStatus.PENDING_REVIEW,
                sourceRefs,
                request.sourceType() == null ? sourceRefs.get(0).type() : request.sourceType(),
                request.sourceId() == null ? sourceRefs.get(0).id() : request.sourceId(),
                generated.questionType() == null ? type : generated.questionType(),
                valueOrDefault(generated.categoryId(), categoryId),
                valueOrDefault(generated.categoryName(), categoryName),
                generated.knowledgePointIds() == null || generated.knowledgePointIds().isEmpty()
                    ? draftKnowledgePointIds
                    : generated.knowledgePointIds(),
                requireGeneratedText(generated.stem(), "question stem"),
                generated.options() == null ? List.of() : generated.options(),
                generated.answer() == null ? List.of() : generated.answer(),
                requireGeneratedText(generated.explanation(), "question explanation"),
                null,
                null,
                now,
                now
            );
            var savedDraft = generatedContents.save(draft);
            taskService.updateTask(
                task,
                TaskLifecycleStatus.SUCCEEDED,
                100,
                "READY",
                new TaskResultRef("GENERATED_CONTENT", savedDraft.id(), null),
                null,
                null,
                task.materialId(),
                savedDraft.id()
            );
            return savedDraft;
        } catch (RuntimeException exception) {
            taskService.updateTask(
                task,
                TaskLifecycleStatus.FAILED,
                100,
                "FAILED",
                null,
                "QUESTION_GENERATION_FAILED",
                safeErrorMessage(exception),
                task.materialId(),
                null
            );
            throw exception;
        }
    }

    public AiNoteDraft generateExplanation(GenerateExplanationRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var point = requireKnowledgePoint(request.knowledgePointId());
        if (!point.knowledgeBaseId().equals(request.knowledgeBaseId())) {
            throw new IllegalArgumentException("Knowledge point is outside knowledge base: " + request.knowledgePointId());
        }
        var sourceRefs = sourceService.ensureUsable(sourceService.normalize(request.knowledgeBaseId(), request.sourceRefs()));
        var task = taskService.startTask(taskService.createTask(
            TaskKind.EXPLANATION_GENERATION,
            request.knowledgeBaseId(),
            sourceService.firstMaterialId(sourceRefs),
            aiProviderType(),
            chatModelName(),
            "GENERATING"
        ), "GENERATING");
        try {
            var generated = aiProvider.generateKnowledgePointExplanation(new AiProvider.KnowledgePointExplanationPrompt(
                request.knowledgeBaseId(),
                point.id(),
                point.name(),
                point.description(),
                sourceRefs,
                request.prompt()
            ));
            var draft = aiNotes.saveDraft(new AiNoteDraft(
                newId("note_draft"),
                request.knowledgeBaseId(),
                task.id(),
                AiNoteType.KNOWLEDGE_POINT_EXPLANATION,
                requireGeneratedText(generated.title(), "explanation title"),
                requireGeneratedText(generated.content(), "explanation content"),
                sourceRefs,
                clock.instant()
            ));
            taskService.updateTask(
                task,
                TaskLifecycleStatus.SUCCEEDED,
                100,
                "READY",
                new TaskResultRef("AI_NOTE_DRAFT", draft.id(), null),
                null,
                null,
                task.materialId(),
                null
            );
            return draft;
        } catch (RuntimeException exception) {
            taskService.updateTask(
                task,
                TaskLifecycleStatus.FAILED,
                100,
                "FAILED",
                null,
                "EXPLANATION_GENERATION_FAILED",
                safeErrorMessage(exception),
                task.materialId(),
                null
            );
            throw exception;
        }
    }

    public AiNoteDraft generateReviewSuggestion(GenerateReviewSuggestionRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var sourceRefs = sourceService.ensureUsable(sourceService.normalize(request.knowledgeBaseId(), request.sourceRefs()));
        var weakPoints = request.weakKnowledgePointIds() == null ? List.<String>of() : request.weakKnowledgePointIds();
        var task = taskService.startTask(taskService.createTask(
            TaskKind.REVIEW_SUGGESTION_GENERATION,
            request.knowledgeBaseId(),
            sourceService.firstMaterialId(sourceRefs),
            aiProviderType(),
            chatModelName(),
            "GENERATING"
        ), "GENERATING");
        try {
            var generated = aiProvider.generateReviewSuggestion(new AiProvider.ReviewSuggestionPrompt(
                request.knowledgeBaseId(),
                sourceRefs,
                weakPoints,
                request.wrongQuestionIds() == null ? List.of() : request.wrongQuestionIds(),
                request.prompt()
            ));
            var draft = aiNotes.saveDraft(new AiNoteDraft(
                newId("note_draft"),
                request.knowledgeBaseId(),
                task.id(),
                AiNoteType.REVIEW_SUGGESTION,
                requireGeneratedText(generated.title(), "review suggestion title"),
                requireGeneratedText(generated.content(), "review suggestion content"),
                sourceRefs,
                clock.instant()
            ));
            taskService.updateTask(
                task,
                TaskLifecycleStatus.SUCCEEDED,
                100,
                "READY",
                new TaskResultRef("AI_NOTE_DRAFT", draft.id(), null),
                null,
                null,
                task.materialId(),
                null
            );
            return draft;
        } catch (RuntimeException exception) {
            taskService.updateTask(
                task,
                TaskLifecycleStatus.FAILED,
                100,
                "FAILED",
                null,
                "REVIEW_SUGGESTION_GENERATION_FAILED",
                safeErrorMessage(exception),
                task.materialId(),
                null
            );
            throw exception;
        }
    }

    public SavedAiNote saveAiNote(SaveAiNoteRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var sourceRefs = sourceService.ensureUsable(sourceService.normalize(request.knowledgeBaseId(), request.sourceRefs()));
        return aiNotes.save(new SavedAiNote(
            newId("note"),
            request.knowledgeBaseId(),
            request.type(),
            request.title(),
            request.content(),
            sourceRefs,
            clock.instant()
        ));
    }

    public List<GeneratedQuestionDraft> listGeneratedContents(GeneratedContentStatus status) {
        return generatedContents.list().stream()
            .filter(content -> status == null || content.status() == status)
            .sorted(Comparator.comparing(GeneratedQuestionDraft::createdAt).reversed())
            .toList();
    }

    public GeneratedQuestionDraft reviewGeneratedContent(
        String generatedContentId,
        ReviewGeneratedContentRequest request
    ) {
        var existing = requireGeneratedContent(generatedContentId);
        if (request.status() == GeneratedContentStatus.SAVED
            && (existing.status() == GeneratedContentStatus.DELETED || existing.status() == GeneratedContentStatus.DISCARDED)) {
            throw new IllegalArgumentException("Deleted or discarded generated content cannot be saved: " + generatedContentId);
        }
        var sourceRefs = request.sourceRefs() == null || request.sourceRefs().isEmpty()
            ? existing.sourceRefs()
            : sourceService.normalize(existing.knowledgeBaseId(), request.sourceRefs());
        var savedQuestionId = existing.savedQuestionId();
        var savedAt = existing.savedAt();
        if (request.status() == GeneratedContentStatus.SAVED && savedQuestionId == null) {
            sourceService.ensureUsable(sourceRefs);
            savedQuestionId = newId("q");
            savedAt = clock.instant();
        } else if (request.status() == GeneratedContentStatus.SAVED) {
            sourceService.ensureUsable(sourceRefs);
        }
        var reviewKnowledgePointIds = request.knowledgePointIds() == null || request.knowledgePointIds().isEmpty()
            ? existing.knowledgePointIds()
            : request.knowledgePointIds().stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        var categoryId = valueOrDefault(request.categoryId(), existing.categoryId());
        var categoryName = valueOrDefault(request.categoryName(), existing.categoryName());
        var updated = new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.generationTaskId(),
            request.status(),
            sourceRefs,
            existing.sourceType(),
            existing.sourceId(),
            existing.questionType(),
            categoryId,
            categoryName,
            reviewKnowledgePointIds,
            valueOrDefault(request.stem(), existing.stem()),
            request.options() == null || request.options().isEmpty() ? existing.options() : request.options(),
            request.answer() == null || request.answer().isEmpty() ? existing.answer() : request.answer(),
            valueOrDefault(request.explanation(), existing.explanation()),
            savedQuestionId,
            savedAt,
            existing.createdAt(),
            clock.instant()
        );
        generatedContents.save(updated);
        if (updated.status() == GeneratedContentStatus.SAVED) {
            questions.save(toQuestionSummary(updated));
        }
        return updated;
    }

    public void deleteGeneratedContent(String generatedContentId) {
        var existing = requireGeneratedContent(generatedContentId);
        generatedContents.save(new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.generationTaskId(),
            GeneratedContentStatus.DELETED,
            existing.sourceRefs(),
            existing.sourceType(),
            existing.sourceId(),
            existing.questionType(),
            existing.categoryId(),
            existing.categoryName(),
            existing.knowledgePointIds(),
            existing.stem(),
            existing.options(),
            existing.answer(),
            existing.explanation(),
            existing.savedQuestionId(),
            existing.savedAt(),
            existing.createdAt(),
            clock.instant()
        ));
    }

    private KnowledgeBase requireKnowledgeBase(String knowledgeBaseId) {
        return knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private GeneratedQuestionDraft requireGeneratedContent(String generatedContentId) {
        return generatedContents.find(generatedContentId)
            .orElseThrow(() -> new IllegalArgumentException("Generated content not found: " + generatedContentId));
    }

    private KnowledgePoint requireKnowledgePoint(String knowledgePointId) {
        return knowledgePoints.find(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
    }

    private QuestionSummary toQuestionSummary(GeneratedQuestionDraft draft) {
        return new QuestionSummary(
            draft.savedQuestionId(),
            draft.knowledgeBaseId(),
            draft.questionType(),
            draft.stem(),
            draft.categoryId(),
            draft.categoryName(),
            null,
            draft.knowledgePointIds(),
            0,
            0.0,
            draft.sourceRefs(),
            draft.createdAt(),
            draft.savedAt()
        );
    }

    private List<String> requestedKnowledgePointIds(List<String> requestedIds, List<SourceRef> sourceRefs) {
        if (requestedIds != null && !requestedIds.isEmpty()) {
            return requestedIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        }
        return knowledgePointIds(sourceRefs);
    }

    private String defaultCategoryId(List<String> knowledgePointIds) {
        return knowledgePointIds == null || knowledgePointIds.isEmpty() ? "uncategorized" : knowledgePointIds.get(0);
    }

    private String defaultCategoryName(String categoryId) {
        return knowledgePoints.find(categoryId)
            .map(KnowledgePoint::name)
            .orElse("Uncategorized");
    }

    private List<String> knowledgePointIds(List<SourceRef> sourceRefs) {
        return sourceRefs.stream()
            .filter(ref -> ref.type() == SourceType.KNOWLEDGE_POINT)
            .map(SourceRef::id)
            .distinct()
            .toList();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String requireGeneratedText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("AiProvider returned blank " + fieldName);
        }
        return value;
    }

    private String safeErrorMessage(RuntimeException exception) {
        var message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return truncate(message);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private AiProviderType aiProviderType() {
        return AiProviderType.OPENAI_COMPATIBLE;
    }

    private String chatModelName() {
        return "openai-compatible-chat";
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

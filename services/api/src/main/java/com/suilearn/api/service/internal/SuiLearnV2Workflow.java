package com.suilearn.api.service.internal;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.RenameKnowledgeBaseRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.dto.SubmitAnswerRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.knowledgepoint.application.KnowledgePointCandidateExtractor;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.AnswerRecord;
import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialDeletionResult;
import com.suilearn.api.model.MaterialDetail;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.RagAnswer;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SuiLearnV2Workflow {
    private static final int MAX_EXTRACTED_POINTS = 16;
    private static final int MAX_EVIDENCE_CHUNKS = 8;
    private static final String KNOWLEDGE_POINT_EXTRACTION_QUERY = "核心知识点 概念 API 原理 面试重点";

    private final AiProvider aiProvider;
    private final Clock clock;
    private final EmbeddingProvider embeddingProvider;
    private final MaterialChunker materialChunker;
    private final MaterialParser materialParser;
    private final Retriever retriever;
    private final SourceService sourceService;
    private final SuiLearnV2Store store;
    private final TaskExecutor taskExecutor;
    private final TaskService taskService;

    public SuiLearnV2Workflow(
        AiProvider aiProvider,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Retriever retriever,
        Clock clock,
        SuiLearnV2Store store,
        TaskService taskService,
        TaskExecutor taskExecutor,
        SourceService sourceService
    ) {
        this.aiProvider = aiProvider;
        this.clock = clock;
        this.embeddingProvider = embeddingProvider;
        this.materialChunker = materialChunker;
        this.materialParser = materialParser;
        this.retriever = retriever;
        this.sourceService = sourceService;
        this.store = store;
        this.taskExecutor = taskExecutor;
        this.taskService = taskService;
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return store.listKnowledgeBases().stream()
            .sorted(Comparator.comparing(KnowledgeBase::createdAt))
            .toList();
    }

    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        var now = clock.instant();
        var knowledgeBase = new KnowledgeBase(newId("kb"), request.name(), request.description(), now, now);
        return store.saveKnowledgeBase(knowledgeBase);
    }

    public KnowledgeBaseDetail getKnowledgeBaseDetail(String knowledgeBaseId) {
        var knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        return new KnowledgeBaseDetail(
            knowledgeBase.id(),
            knowledgeBase.name(),
            knowledgeBase.description(),
            knowledgeBase.createdAt(),
            knowledgeBase.updatedAt(),
            countMaterials(knowledgeBaseId),
            countKnowledgePoints(knowledgeBaseId),
            countSavedQuestions(knowledgeBaseId),
            countGeneratedContents(knowledgeBaseId),
            countAiNotes(knowledgeBaseId)
        );
    }

    public KnowledgeBase renameKnowledgeBase(String knowledgeBaseId, RenameKnowledgeBaseRequest request) {
        var existing = requireKnowledgeBase(knowledgeBaseId);
        var updated = new KnowledgeBase(
            existing.id(),
            request.name(),
            request.description(),
            existing.createdAt(),
            clock.instant()
        );
        return store.saveKnowledgeBase(updated);
    }

    public void deleteKnowledgeBase(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        store.deleteKnowledgeBase(knowledgeBaseId);
    }

    public TaskStatus getTaskStatus(String taskId) {
        return taskService.getTaskStatus(taskId);
    }

    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return store.listMaterials(knowledgeBaseId).stream()
            .sorted(Comparator.comparing(LearningMaterial::createdAt))
            .toList();
    }

    public LearningMaterial importMaterial(String knowledgeBaseId, ImportMaterialRequest request) {
        requireKnowledgeBase(knowledgeBaseId);
        var importTask = taskService.createTask(
            TaskKind.MATERIAL_IMPORT,
            knowledgeBaseId,
            null,
            null,
            null,
            "UPLOADED"
        );
        var material = new LearningMaterial(
            newId("mat"),
            knowledgeBaseId,
            request.title(),
            request.sourceType(),
            MaterialStatus.UPLOADED,
            importTask.id(),
            null,
            null,
            request.content(),
            clock.instant(),
            null
        );
        var saved = store.saveMaterial(material);
        var materialRef = new AtomicReference<>(saved);
        var embeddingTaskRef = new AtomicReference<TaskStatus>();
        return taskExecutor.runManagedTask(
            importTask,
            "UPLOADED",
            importExecution -> {
                var parsing = store.saveMaterial(withStatus(saved, MaterialStatus.PARSING));
                materialRef.set(parsing);
                importExecution.progress(20, "PARSING", parsing.id(), null);
                var parsed = materialParser.parse(new MaterialParser.ParseRequest(
                    parsing.title(),
                    request.fileName(),
                    parsing.sourceType(),
                    parsing.content()
                ));
                var chunking = store.saveMaterial(withContentAndStatus(
                    parsing,
                    parsed.content(),
                    MaterialStatus.CHUNKING
                ));
                materialRef.set(chunking);
                importExecution.progress(45, "CHUNKING", chunking.id(), null);
                var chunks = materialChunker.chunk(chunking);
                var embeddingTask = taskService.createTask(
                    TaskKind.EMBEDDING,
                    knowledgeBaseId,
                    chunking.id(),
                    null,
                    embeddingProvider.model(),
                    "INDEXING"
                );
                embeddingTaskRef.set(embeddingTask);
                var ready = taskExecutor.runManagedTask(
                    embeddingTask,
                    "INDEXING",
                    embeddingExecution -> {
                        var indexing = store.saveMaterial(withEmbeddingTaskId(withStatus(chunking, MaterialStatus.INDEXING), embeddingExecution.current().id()));
                        materialRef.set(indexing);
                        store.saveChunks(indexing.id(), chunks.stream().map(this::withEmbedding).toList());
                        var indexed = store.saveMaterial(withStatus(indexing, MaterialStatus.READY));
                        materialRef.set(indexed);
                        embeddingExecution.succeed(
                            "READY",
                            new TaskResultRef("MATERIAL_CHUNKS", indexed.id(), chunks.size()),
                            indexed.id(),
                            null
                        );
                        return indexed;
                    },
                    (embeddingExecution, exception) -> {
                        embeddingExecution.fail(
                            "EMBEDDING_FAILED",
                            safeErrorMessage(exception),
                            materialRef.get().id(),
                            null
                        );
                        throw exception;
                    }
                );
                importExecution.succeed(
                    "READY",
                    new TaskResultRef("MATERIAL", ready.id(), null),
                    ready.id(),
                    null
                );
                return ready;
            },
            (importExecution, exception) -> {
                var lastMaterial = materialRef.get();
                var failedMaterial = embeddingTaskRef.get() == null
                    ? lastMaterial
                    : withEmbeddingTaskId(lastMaterial, embeddingTaskRef.get().id());
                var failed = store.saveMaterial(withStatusAndError(
                    failedMaterial,
                    MaterialStatus.FAILED,
                    safeErrorMessage(exception)
                ));
                importExecution.fail(
                    "MATERIAL_IMPORT_FAILED",
                    safeErrorMessage(exception),
                    failed.id(),
                    null
                );
                return failed;
            }
        );
    }

    public MaterialDetail getMaterialDetail(String materialId) {
        var material = requireMaterial(materialId);
        return new MaterialDetail(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
            material.importTaskId(),
            material.embeddingTaskId(),
            material.errorMessage(),
            material.content(),
            truncate(material.content()),
            material.createdAt(),
            material.deletedAt(),
            store.listChunksByMaterial(material.id()),
            store.listKnowledgePoints().stream()
                .filter(point -> material.id().equals(point.sourceMaterialId()))
                .sorted(Comparator.comparing(KnowledgePoint::name))
                .toList()
        );
    }

    public MaterialDeletionResult deleteMaterial(
        String materialId,
        DeletedMaterialSavedContentPolicy savedContentPolicy,
        DeletedMaterialPendingContentPolicy pendingContentPolicy
    ) {
        var material = requireMaterial(materialId);
        var effectiveSavedPolicy = savedContentPolicy == null
            ? DeletedMaterialSavedContentPolicy.KEEP_SAVED_CONTENT
            : savedContentPolicy;
        var effectivePendingPolicy = pendingContentPolicy == null
            ? DeletedMaterialPendingContentPolicy.DELETE_PENDING_GENERATED_CONTENT
            : pendingContentPolicy;
        var deletedAt = clock.instant();
        var updatedMaterial = new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            MaterialStatus.DELETED,
            material.importTaskId(),
            material.embeddingTaskId(),
            material.errorMessage(),
            material.content(),
            material.createdAt(),
            deletedAt
        );
        store.saveMaterial(updatedMaterial);

        var invalidatedChunkCount = store.invalidateChunksByMaterial(materialId);
        var deletedPendingCount = 0;
        for (var content : store.listGeneratedContents()) {
            if (!sourceService.referencesMaterial(content.sourceRefs(), materialId)) {
                continue;
            }
            if (content.status() == GeneratedContentStatus.PENDING_REVIEW
                && effectivePendingPolicy == DeletedMaterialPendingContentPolicy.DELETE_PENDING_GENERATED_CONTENT) {
                store.saveGeneratedContent(updateGeneratedStatus(content, GeneratedContentStatus.DELETED));
                deletedPendingCount++;
            } else {
                store.saveGeneratedContent(withSourceRefs(content, sourceService.markMaterialDeleted(content.sourceRefs(), materialId)));
            }
        }

        var retainedSavedQuestionCount = 0;
        for (var question : store.listQuestions()) {
            if (!sourceService.referencesMaterial(question.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                store.deleteQuestion(question.id());
            } else {
                store.saveQuestion(withSourceRefs(question, sourceService.markMaterialDeleted(question.sourceRefs(), materialId)));
                retainedSavedQuestionCount++;
            }
        }

        var retainedAiNoteCount = 0;
        for (var note : store.listAiNotes()) {
            if (!sourceService.referencesMaterial(note.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                store.deleteAiNote(note.id());
            } else {
                store.saveAiNote(withSourceRefs(note, sourceService.markMaterialDeleted(note.sourceRefs(), materialId)));
                retainedAiNoteCount++;
            }
        }

        return new MaterialDeletionResult(
            materialId,
            MaterialStatus.DELETED,
            effectiveSavedPolicy,
            effectivePendingPolicy,
            invalidatedChunkCount,
            deletedPendingCount,
            retainedSavedQuestionCount,
            retainedAiNoteCount,
            deletedAt
        );
    }

    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId) {
        var material = requireMaterial(materialId);
        var task = taskService.startTask(taskService.createTask(
            TaskKind.KNOWLEDGE_POINT_EXTRACTION,
            material.knowledgeBaseId(),
            material.id(),
            aiProviderType(),
            chatModelName(),
            "EXTRACTING"
        ), "EXTRACTING");
        var evidence = extractionEvidence(material);
        var candidates = extractCandidateTerms(material, evidence);
        var sourceRefs = evidence.isEmpty()
            ? List.of(sourceService.materialSourceRef(material))
            : evidence.stream().map(MaterialChunk::sourceRef).toList();
        var extracted = candidates.stream()
            .map(candidate -> new KnowledgePoint(
                newId("kp"),
                material.knowledgeBaseId(),
                candidate.name(),
                candidate.description(),
                material.id(),
                sourceRefs
            ))
            .toList();
        extracted.forEach(store::saveKnowledgePoint);
        var finished = taskService.updateTask(
            task,
            TaskLifecycleStatus.SUCCEEDED,
            100,
            "READY",
            new TaskResultRef("KNOWLEDGE_POINTS", material.id(), extracted.size()),
            null,
            null,
            material.id(),
            null
        );
        return new KnowledgePointExtractionResult(finished.id(), finished, extracted);
    }

    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return store.listKnowledgePoints(knowledgeBaseId).stream()
            .sorted(Comparator.comparing(KnowledgePoint::name))
            .toList();
    }

    public KnowledgePoint updateKnowledgePoint(String knowledgePointId, UpdateKnowledgePointRequest request) {
        var existing = requireKnowledgePoint(knowledgePointId);
        var updated = new KnowledgePoint(
            existing.id(),
            existing.knowledgeBaseId(),
            request.name(),
            request.description(),
            existing.sourceMaterialId(),
            existing.sourceRefs()
        );
        return store.saveKnowledgePoint(updated);
    }

    public void deleteKnowledgePoint(String knowledgePointId) {
        requireKnowledgePoint(knowledgePointId);
        store.deleteKnowledgePoint(knowledgePointId);
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
            var savedDraft = store.saveGeneratedContent(draft);
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

    public List<GeneratedQuestionDraft> listGeneratedContents(GeneratedContentStatus status) {
        return store.listGeneratedContents().stream()
            .filter(content -> status == null || content.status() == status)
            .sorted(Comparator.comparing(GeneratedQuestionDraft::createdAt).reversed())
            .toList();
    }

    public List<QuestionSummary> listQuestions(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return store.listQuestions(knowledgeBaseId).stream()
            .sorted(Comparator.comparing(QuestionSummary::createdAt).reversed())
            .toList();
    }

    public KnowledgeBaseStatistics getStatistics(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        var questionCount = countSavedQuestions(knowledgeBaseId);
        var answerRecords = store.listAnswerRecords(knowledgeBaseId);
        var answeredQuestionCount = (int) answerRecords.stream().map(AnswerRecord::questionId).distinct().count();
        var answerCount = answerRecords.size();
        var correctRate = answerCount == 0
            ? null
            : answerRecords.stream().filter(AnswerRecord::correct).count() / (double) answerCount;
        var wrongQuestionIds = answerRecords.stream()
            .filter(record -> !record.correct())
            .map(AnswerRecord::questionId)
            .distinct()
            .toList();
        var activeMaterials = store.listMaterials(knowledgeBaseId).stream()
            .filter(material -> material.status() != MaterialStatus.DELETED)
            .toList();
        return new KnowledgeBaseStatistics(
            knowledgeBaseId,
            questionCount,
            activeMaterials.size(),
            (int) activeMaterials.stream().filter(material -> material.status() == MaterialStatus.READY).count(),
            countKnowledgePoints(knowledgeBaseId),
            (int) store.listGeneratedContents().stream()
                .filter(content -> content.knowledgeBaseId().equals(knowledgeBaseId))
                .filter(content -> content.status() == GeneratedContentStatus.PENDING_REVIEW)
                .count(),
            countAiNotes(knowledgeBaseId),
            answeredQuestionCount,
            answerCount,
            correctRate,
            wrongQuestionIds.size(),
            store.listQuestions(knowledgeBaseId).stream()
                .filter(question -> wrongQuestionIds.contains(question.id()))
                .flatMap(question -> question.knowledgePointIds().stream())
                .distinct()
                .limit(3)
                .toList()
        );
    }

    public AnswerRecord submitAnswer(String knowledgeBaseId, SubmitAnswerRequest request) {
        requireKnowledgeBase(knowledgeBaseId);
        var question = store.listQuestions(knowledgeBaseId).stream()
            .filter(candidate -> candidate.id().equals(request.questionId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Question not found in knowledge base: " + request.questionId()));
        var record = store.saveAnswerRecord(new AnswerRecord(
            newId("answer"),
            knowledgeBaseId,
            question.id(),
            request.userAnswer(),
            request.correct(),
            Math.max(0, request.durationMs()),
            clock.instant()
        ));
        refreshQuestionStats(question);
        return record;
    }

    private void refreshQuestionStats(QuestionSummary question) {
        var records = store.listAnswerRecordsByQuestion(question.id());
        var answeredCount = records.size();
        var correctRate = answeredCount == 0
            ? 0.0
            : records.stream().filter(AnswerRecord::correct).count() / (double) answeredCount;
        store.saveQuestion(new QuestionSummary(
            question.id(),
            question.knowledgeBaseId(),
            question.questionType(),
            question.stem(),
            question.categoryId(),
            question.categoryName(),
            question.difficulty(),
            question.knowledgePointIds(),
            answeredCount,
            correctRate,
            question.sourceRefs(),
            question.createdAt(),
            question.savedAt()
        ));
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
            var draft = store.saveAiNoteDraft(new AiNoteDraft(
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
            var draft = store.saveAiNoteDraft(new AiNoteDraft(
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
        var note = new SavedAiNote(
            newId("note"),
            request.knowledgeBaseId(),
            request.type(),
            request.title(),
            request.content(),
            sourceRefs,
            clock.instant()
        );
        return store.saveAiNote(note);
    }

    public GeneratedQuestionDraft reviewGeneratedContent(String generatedContentId, ReviewGeneratedContentRequest request) {
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
        store.saveGeneratedContent(updated);
        if (updated.status() == GeneratedContentStatus.SAVED) {
            store.saveQuestion(toQuestionSummary(updated));
        }
        return updated;
    }

    public void deleteGeneratedContent(String generatedContentId) {
        var existing = requireGeneratedContent(generatedContentId);
        var deleted = new GeneratedQuestionDraft(
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
        );
        store.saveGeneratedContent(deleted);
    }

    public List<SearchResult> search(String query, String knowledgeBaseId, String materialId) {
        return search(query, knowledgeBaseId, materialId, Retriever.RetrievalRequest.DEFAULT_LIMIT);
    }

    public List<SearchResult> search(String query, String knowledgeBaseId, String materialId, Integer limit) {
        var scope = requireSearchScope(knowledgeBaseId, materialId);
        var effectiveLimit = limit == null ? Retriever.RetrievalRequest.DEFAULT_LIMIT : limit;
        return retriever.search(new Retriever.RetrievalRequest(
            query,
            scope.knowledgeBaseId(),
            scope.materialId(),
            effectiveLimit
        ));
    }

    public RagAnswer ask(String question, String knowledgeBaseId, String materialId) {
        if (isBlank(knowledgeBaseId) && isBlank(materialId)) {
            throw new IllegalArgumentException("At least one scope is required: knowledgeBaseId or materialId");
        }
        var scopedKnowledgeBaseId = knowledgeBaseId;
        if (materialId != null && !materialId.isBlank()) {
            var material = requireMaterial(materialId);
            if (material.status() == MaterialStatus.DELETED) {
                return new RagAnswer("不确定：资料已删除，无法作为回答依据。", true, List.of(), List.of(), null);
            }
            if (scopedKnowledgeBaseId != null
                && !scopedKnowledgeBaseId.isBlank()
                && !material.knowledgeBaseId().equals(scopedKnowledgeBaseId)) {
                throw new IllegalArgumentException("Material is outside knowledge base: " + materialId);
            }
            scopedKnowledgeBaseId = material.knowledgeBaseId();
        } else if (scopedKnowledgeBaseId != null && !scopedKnowledgeBaseId.isBlank()) {
            requireKnowledgeBase(scopedKnowledgeBaseId);
        }
        var citations = retriever.retrieveEvidence(
            new Retriever.RetrievalRequest(question, scopedKnowledgeBaseId, materialId),
            3
        );
        if (citations.isEmpty()) {
            return new RagAnswer("不确定：资料中未找到明确依据。", true, List.of(), List.of(), null);
        }
        return new RagAnswer(
            "根据已导入资料，建议优先查看引用片段并结合原文复核。",
            false,
            citations.stream().map(MaterialChunk::sourceRef).toList(),
            citations,
            null
        );
    }

    private SearchScope requireSearchScope(String knowledgeBaseId, String materialId) {
        if (isBlank(knowledgeBaseId) && isBlank(materialId)) {
            throw new IllegalArgumentException("At least one scope is required: knowledgeBaseId or materialId");
        }
        if (!isBlank(materialId)) {
            var material = requireMaterial(materialId);
            if (!isBlank(knowledgeBaseId) && !material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("Material is outside knowledge base: " + materialId);
            }
            return new SearchScope(material.knowledgeBaseId(), material.id());
        }
        requireKnowledgeBase(knowledgeBaseId);
        return new SearchScope(knowledgeBaseId, null);
    }

    private record SearchScope(String knowledgeBaseId, String materialId) {
    }

    private KnowledgeBase requireKnowledgeBase(String knowledgeBaseId) {
        return store.findKnowledgeBase(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return store.findMaterial(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
    }

    private GeneratedQuestionDraft requireGeneratedContent(String generatedContentId) {
        return store.findGeneratedContent(generatedContentId)
            .orElseThrow(() -> new IllegalArgumentException("Generated content not found: " + generatedContentId));
    }

    private KnowledgePoint requireKnowledgePoint(String knowledgePointId) {
        return store.findKnowledgePoint(knowledgePointId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge point not found: " + knowledgePointId));
    }

    private int countMaterials(String knowledgeBaseId) {
        return store.listMaterials(knowledgeBaseId).size();
    }

    private int countKnowledgePoints(String knowledgeBaseId) {
        return store.listKnowledgePoints(knowledgeBaseId).size();
    }

    private int countGeneratedContents(String knowledgeBaseId) {
        return (int) store.listGeneratedContents().stream()
            .filter(content -> content.knowledgeBaseId().equals(knowledgeBaseId))
            .filter(content -> content.status() != GeneratedContentStatus.DELETED)
            .count();
    }

    private int countSavedQuestions(String knowledgeBaseId) {
        return store.listQuestions(knowledgeBaseId).size();
    }

    private int countAiNotes(String knowledgeBaseId) {
        return store.listAiNotes(knowledgeBaseId).size();
    }

    private LearningMaterial withStatus(LearningMaterial material, MaterialStatus status) {
        return withContentAndStatus(material, material.content(), status);
    }

    private LearningMaterial withStatusAndError(LearningMaterial material, MaterialStatus status, String errorMessage) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            status,
            material.importTaskId(),
            material.embeddingTaskId(),
            errorMessage,
            material.content(),
            material.createdAt(),
            material.deletedAt()
        );
    }

    private LearningMaterial withContentAndStatus(LearningMaterial material, String content, MaterialStatus status) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            status,
            material.importTaskId(),
            material.embeddingTaskId(),
            status == MaterialStatus.FAILED ? material.errorMessage() : null,
            content,
            material.createdAt(),
            material.deletedAt()
        );
    }

    private LearningMaterial withEmbeddingTaskId(LearningMaterial material, String embeddingTaskId) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
            material.importTaskId(),
            embeddingTaskId,
            material.errorMessage(),
            material.content(),
            material.createdAt(),
            material.deletedAt()
        );
    }

    private MaterialChunk withEmbedding(MaterialChunk chunk) {
        var embedding = embeddingProvider.embed(chunk.content()).values();
        return new MaterialChunk(
            chunk.id(),
            chunk.knowledgeBaseId(),
            chunk.materialId(),
            chunk.content(),
            chunk.ordinal(),
            chunk.sourceRef(),
            embedding,
            EmbeddingStatus.READY,
            embeddingProvider.model(),
            embedding.size()
        );
    }

    private List<ExtractedKnowledgePointCandidate> extractCandidateTerms(
        LearningMaterial material,
        List<MaterialChunk> evidence
    ) {
        var generated = aiProvider.extractKnowledgePoints(new AiProvider.KnowledgePointExtractionPrompt(
            material.knowledgeBaseId(),
            material.id(),
            material.title(),
            evidence.stream().map(MaterialChunk::sourceRef).toList(),
            MAX_EXTRACTED_POINTS
        ));
        var candidates = new LinkedHashMap<String, ExtractedKnowledgePointCandidate>();
        if (generated != null) {
            for (var point : generated) {
                addKnowledgePointCandidate(candidates, point.name(), point.description(), material.title());
            }
        }
        if (candidates.isEmpty()) {
            for (var term : KnowledgePointCandidateExtractor.extract(material.content())) {
                addKnowledgePointCandidate(candidates, term, null, material.title());
            }
        }
        return candidates.values().stream().limit(MAX_EXTRACTED_POINTS).toList();
    }

    private List<MaterialChunk> extractionEvidence(LearningMaterial material) {
        var retrieved = retriever.retrieveEvidence(
            new Retriever.RetrievalRequest(
                KNOWLEDGE_POINT_EXTRACTION_QUERY,
                material.knowledgeBaseId(),
                material.id()
            ),
            MAX_EVIDENCE_CHUNKS
        );
        var byId = new LinkedHashMap<String, MaterialChunk>();
        retrieved.stream()
            .filter(chunk -> material.id().equals(chunk.materialId()))
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        store.listChunksByMaterial(material.id()).stream()
            .limit(MAX_EVIDENCE_CHUNKS)
            .forEach(chunk -> byId.putIfAbsent(chunk.id(), chunk));
        return byId.values().stream().limit(MAX_EVIDENCE_CHUNKS).toList();
    }

    private void addKnowledgePointCandidate(
        LinkedHashMap<String, ExtractedKnowledgePointCandidate> candidates,
        String rawName,
        String rawDescription,
        String materialTitle
    ) {
        if (candidates.size() >= MAX_EXTRACTED_POINTS) {
            return;
        }
        var name = sanitizeKnowledgePointName(rawName);
        if (!isUsableKnowledgePointName(name)) {
            return;
        }
        var description = rawDescription == null || rawDescription.isBlank()
            ? "基于资料《" + materialTitle + "》的证据片段提炼。"
            : rawDescription.trim();
        candidates.putIfAbsent(normalizeKnowledgePointKey(name), new ExtractedKnowledgePointCandidate(name, description));
    }

    private String sanitizeKnowledgePointName(String rawName) {
        return rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
    }

    private boolean isUsableKnowledgePointName(String name) {
        return name.length() >= 2
            && name.length() <= 32
            && name.codePoints().anyMatch(codePoint ->
                Character.isLetterOrDigit(codePoint) || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
            )
            && !name.matches("[-_=~—–]+")
            && !name.matches(".*[。！？!?；;，,、].*")
            && !name.contains("——")
            && !name.contains("--");
    }

    private String normalizeKnowledgePointKey(String term) {
        return term.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private GeneratedQuestionDraft updateGeneratedStatus(GeneratedQuestionDraft existing, GeneratedContentStatus status) {
        return new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.generationTaskId(),
            status,
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
        );
    }

    private GeneratedQuestionDraft withSourceRefs(GeneratedQuestionDraft existing, List<SourceRef> sourceRefs) {
        return new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.generationTaskId(),
            existing.status(),
            sourceRefs,
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
        );
    }

    private QuestionSummary withSourceRefs(QuestionSummary existing, List<SourceRef> sourceRefs) {
        return new QuestionSummary(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.questionType(),
            existing.stem(),
            existing.categoryId(),
            existing.categoryName(),
            existing.difficulty(),
            existing.knowledgePointIds(),
            existing.answeredCount(),
            existing.correctRate(),
            sourceRefs,
            existing.createdAt(),
            existing.savedAt()
        );
    }

    private SavedAiNote withSourceRefs(SavedAiNote existing, List<SourceRef> sourceRefs) {
        return new SavedAiNote(
            existing.id(),
            existing.knowledgeBaseId(),
            existing.type(),
            existing.title(),
            existing.content(),
            sourceRefs,
            existing.savedAt()
        );
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
        return store.findKnowledgePoint(categoryId)
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

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private AiProviderType aiProviderType() {
        return AiProviderType.OPENAI_COMPATIBLE;
    }

    private String chatModelName() {
        return "openai-compatible-chat";
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private record ExtractedKnowledgePointCandidate(String name, String description) {
    }
}


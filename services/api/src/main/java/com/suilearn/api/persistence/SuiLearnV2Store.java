package com.suilearn.api.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.AiProviderType;
import com.suilearn.api.model.AnswerRecord;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointReviewStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskResultRef;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.entity.AiNoteDraftEntity;
import com.suilearn.api.persistence.entity.AiNoteEntity;
import com.suilearn.api.persistence.entity.AnswerRecordEntity;
import com.suilearn.api.persistence.entity.GeneratedContentEntity;
import com.suilearn.api.persistence.entity.KnowledgeBaseEntity;
import com.suilearn.api.persistence.entity.KnowledgePointEntity;
import com.suilearn.api.persistence.entity.LearningMaterialEntity;
import com.suilearn.api.persistence.entity.MaterialChunkEntity;
import com.suilearn.api.persistence.entity.QuestionEntity;
import com.suilearn.api.persistence.entity.TaskStatusEntity;
import com.suilearn.api.persistence.repository.AiNoteDraftJpaRepository;
import com.suilearn.api.persistence.repository.AiNoteJpaRepository;
import com.suilearn.api.persistence.repository.AnswerRecordJpaRepository;
import com.suilearn.api.persistence.repository.GeneratedContentJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgeBaseJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgePointJpaRepository;
import com.suilearn.api.persistence.repository.LearningMaterialJpaRepository;
import com.suilearn.api.persistence.repository.MaterialChunkJpaRepository;
import com.suilearn.api.persistence.repository.QuestionJpaRepository;
import com.suilearn.api.persistence.repository.TaskStatusJpaRepository;
import com.suilearn.api.retrieval.TextSearchTokenizer;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SuiLearnV2Store {
    private static final TypeReference<List<SourceRef>> SOURCE_REFS = new TypeReference<>() {
    };
    private static final TypeReference<List<Double>> DOUBLES = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    private final KnowledgeBaseJpaRepository knowledgeBases;
    private final LearningMaterialJpaRepository materials;
    private final MaterialChunkJpaRepository chunks;
    private final KnowledgePointJpaRepository knowledgePoints;
    private final GeneratedContentJpaRepository generatedContents;
    private final QuestionJpaRepository questions;
    private final AiNoteDraftJpaRepository aiNoteDrafts;
    private final AiNoteJpaRepository aiNotes;
    private final AnswerRecordJpaRepository answerRecords;
    private final TaskStatusJpaRepository tasks;
    private final ObjectMapper objectMapper;
    private final TextSearchTokenizer searchTokenizer;

    public SuiLearnV2Store(
        KnowledgeBaseJpaRepository knowledgeBases,
        LearningMaterialJpaRepository materials,
        MaterialChunkJpaRepository chunks,
        KnowledgePointJpaRepository knowledgePoints,
        GeneratedContentJpaRepository generatedContents,
        QuestionJpaRepository questions,
        AiNoteDraftJpaRepository aiNoteDrafts,
        AiNoteJpaRepository aiNotes,
        AnswerRecordJpaRepository answerRecords,
        TaskStatusJpaRepository tasks,
        ObjectMapper objectMapper,
        TextSearchTokenizer searchTokenizer
    ) {
        this.knowledgeBases = knowledgeBases;
        this.materials = materials;
        this.chunks = chunks;
        this.knowledgePoints = knowledgePoints;
        this.generatedContents = generatedContents;
        this.questions = questions;
        this.aiNoteDrafts = aiNoteDrafts;
        this.aiNotes = aiNotes;
        this.answerRecords = answerRecords;
        this.tasks = tasks;
        this.objectMapper = objectMapper;
        this.searchTokenizer = searchTokenizer;
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBases.findAll().stream().map(this::toModel).toList();
    }

    public Optional<KnowledgeBase> findKnowledgeBase(String id) {
        return knowledgeBases.findById(id).map(this::toModel);
    }

    public KnowledgeBase saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        return toModel(knowledgeBases.save(new KnowledgeBaseEntity(
            knowledgeBase.id(),
            knowledgeBase.name(),
            knowledgeBase.description(),
            knowledgeBase.createdAt(),
            knowledgeBase.updatedAt()
        )));
    }

    @Transactional
    public void deleteKnowledgeBase(String knowledgeBaseId) {
        var materialIds = materials.findByKnowledgeBaseId(knowledgeBaseId).stream()
            .map(LearningMaterialEntity::getId)
            .toList();
        if (!materialIds.isEmpty()) {
            chunks.deleteByMaterialIdIn(materialIds);
        }
        materials.deleteByKnowledgeBaseId(knowledgeBaseId);
        knowledgePoints.deleteByKnowledgeBaseId(knowledgeBaseId);
        generatedContents.deleteByKnowledgeBaseId(knowledgeBaseId);
        questions.deleteByKnowledgeBaseId(knowledgeBaseId);
        aiNoteDrafts.deleteByKnowledgeBaseId(knowledgeBaseId);
        aiNotes.deleteByKnowledgeBaseId(knowledgeBaseId);
        answerRecords.deleteByKnowledgeBaseId(knowledgeBaseId);
        tasks.deleteByKnowledgeBaseId(knowledgeBaseId);
        knowledgeBases.deleteById(knowledgeBaseId);
    }

    @Transactional(readOnly = true)
    public List<LearningMaterial> listMaterials() {
        return materials.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        return materials.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public Optional<LearningMaterial> findMaterial(String id) {
        return materials.findById(id).map(this::toModel);
    }

    public LearningMaterial saveMaterial(LearningMaterial material) {
        return toModel(materials.save(new LearningMaterialEntity(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType().name(),
            material.status().name(),
            material.importTaskId(),
            material.embeddingTaskId(),
            material.errorMessage(),
            material.content(),
            material.createdAt(),
            material.deletedAt(),
            material.currentRevisionId()
        )));
    }

    @Transactional
    public void saveChunks(String materialId, List<MaterialChunk> materialChunks) {
        chunks.deleteByMaterialId(materialId);
        chunks.saveAll(materialChunks.stream()
            .map(chunk -> new MaterialChunkEntity(
                chunk.id(),
                chunk.knowledgeBaseId(),
                chunk.materialId(),
                chunk.content(),
                searchTokenizer.searchText(chunk.content()),
                chunk.ordinal(),
                write(chunk.sourceRef()),
                write(chunk.embedding()),
                chunk.embeddingStatus().name(),
                chunk.embeddingModel(),
                chunk.embeddingDimensions()
            ))
            .toList());
    }

    @Transactional
    public int invalidateChunksByMaterial(String materialId) {
        var existing = chunks.findByMaterialId(materialId);
        if (existing.isEmpty()) {
            return 0;
        }
        chunks.saveAll(existing.stream()
            .map(chunk -> new MaterialChunkEntity(
                chunk.getId(),
                chunk.getKnowledgeBaseId(),
                chunk.getMaterialId(),
                chunk.getContent(),
                chunk.getSearchText(),
                chunk.getOrdinal(),
                chunk.getSourceRefJson(),
                null,
                EmbeddingStatus.INVALIDATED.name(),
                chunk.getEmbeddingModel(),
                chunk.getEmbeddingDimensions()
            ))
            .toList());
        return existing.size();
    }

    @Transactional(readOnly = true)
    public List<MaterialChunk> listChunks() {
        return chunks.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialChunk> listChunksByMaterial(String materialId) {
        return chunks.findByMaterialId(materialId).stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialChunk> searchChunksText(String query, String knowledgeBaseId, String materialId, int limit) {
        var tsquery = searchTokenizer.tsquery(query);
        if (tsquery.isBlank()) {
            return List.of();
        }
        try {
            return chunks.searchText(tsquery, blankToNull(knowledgeBaseId), blankToNull(materialId), Math.max(1, limit))
                .stream()
                .map(this::toModel)
                .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<MaterialChunk> listChunksByScope(String knowledgeBaseId, String materialId) {
        return chunks.findByScope(blankToNull(knowledgeBaseId), blankToNull(materialId)).stream()
            .map(this::toModel)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MaterialChunk> findChunk(String id) {
        return chunks.findById(id).map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<KnowledgePoint> listKnowledgePoints() {
        return knowledgePoints.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        return knowledgePoints.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public Optional<KnowledgePoint> findKnowledgePoint(String id) {
        return knowledgePoints.findById(id).map(this::toModel);
    }

    public KnowledgePoint saveKnowledgePoint(KnowledgePoint point) {
        var entity = new KnowledgePointEntity(point.id(), point.knowledgeBaseId(), point.name(), point.description(),
            point.shortSummary(), point.definition(), write(point.principles()), write(point.applicationScenarios()), write(point.pitfalls()),
            point.reviewStatus().name(), point.sourceOutdated(), point.legacy(), point.sourceMaterialId(), write(point.sourceRefs()));
        entity.setTitle(point.title());
        return toModel(knowledgePoints.save(entity));
    }

    public void deleteKnowledgePoint(String id) {
        knowledgePoints.deleteById(id);
    }

    @Transactional
    public void markKnowledgePointsSourceOutdated(String materialId, String currentRevisionId) {
        knowledgePoints.findAll().stream().map(this::toModel)
            .filter(point -> materialId.equals(point.sourceMaterialId()))
            .filter(point -> point.sourceRefs().stream().anyMatch(ref -> materialId.equals(ref.materialId()) && !currentRevisionId.equals(ref.revisionId())))
            .forEach(point -> saveKnowledgePoint(new KnowledgePoint(point.id(), point.knowledgeBaseId(), point.name(), point.description(),
                point.sourceMaterialId(), point.sourceRefs(), point.title(), point.shortSummary(), point.definition(), point.principles(),
                point.applicationScenarios(), point.pitfalls(), point.reviewStatus(), true, point.legacy())));
    }

    @Transactional(readOnly = true)
    public List<GeneratedQuestionDraft> listGeneratedContents() {
        return generatedContents.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedQuestionDraft> findGeneratedContent(String id) {
        return generatedContents.findById(id).map(this::toModel);
    }

    public GeneratedQuestionDraft saveGeneratedContent(GeneratedQuestionDraft draft) {
        var entity = new GeneratedContentEntity(
            draft.id(),
            draft.knowledgeBaseId(),
            draft.generationTaskId(),
            draft.status().name(),
            write(draft.sourceRefs()),
            draft.sourceType() == null ? null : draft.sourceType().name(),
            draft.sourceId(),
            draft.questionType().name(),
            draft.categoryId(),
            draft.categoryName(),
            write(draft.knowledgePointIds()),
            draft.stem(),
            write(draft.options()),
            write(draft.answer()),
            draft.explanation(),
            draft.savedQuestionId(),
            draft.savedAt(),
            draft.createdAt(),
            draft.updatedAt()
        );
        entity.setEvidence(draft.knowledgePointId(), draft.materialId(), draft.revisionId(), draft.evidenceExcerpt());
        return toModel(generatedContents.save(entity));
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> listQuestions() {
        return questions.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> listQuestions(String knowledgeBaseId) {
        return questions.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    public void deleteQuestion(String id) {
        questions.deleteById(id);
    }

    public QuestionSummary saveQuestion(QuestionSummary question) {
        return toModel(questions.save(new QuestionEntity(
            question.id(),
            question.knowledgeBaseId(),
            question.questionType().name(),
            question.stem(),
            question.categoryId(),
            question.categoryName(),
            question.difficulty(),
            write(question.knowledgePointIds()),
            question.answeredCount(),
            question.correctRate(),
            write(question.sourceRefs()),
            question.createdAt(),
            question.savedAt()
        )));
    }

    @Transactional(readOnly = true)
    public List<AnswerRecord> listAnswerRecords(String knowledgeBaseId) {
        return answerRecords.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<AnswerRecord> listAnswerRecordsByQuestion(String questionId) {
        return answerRecords.findByQuestionId(questionId).stream().map(this::toModel).toList();
    }

    public AnswerRecord saveAnswerRecord(AnswerRecord answerRecord) {
        return toModel(answerRecords.save(new AnswerRecordEntity(
            answerRecord.id(),
            answerRecord.knowledgeBaseId(),
            answerRecord.questionId(),
            write(answerRecord.userAnswer()),
            answerRecord.correct(),
            answerRecord.durationMs(),
            answerRecord.answeredAt()
        )));
    }

    @Transactional(readOnly = true)
    public List<AiNoteDraft> listAiNoteDrafts(String knowledgeBaseId) {
        return aiNoteDrafts.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    public AiNoteDraft saveAiNoteDraft(AiNoteDraft note) {
        return toModel(aiNoteDrafts.save(new AiNoteDraftEntity(
            note.id(),
            note.knowledgeBaseId(),
            note.generationTaskId(),
            note.type().name(),
            note.title(),
            note.content(),
            write(note.sourceRefs()),
            note.createdAt()
        )));
    }

    @Transactional(readOnly = true)
    public List<SavedAiNote> listAiNotes() {
        return aiNotes.findAll().stream().map(this::toModel).toList();
    }

    @Transactional(readOnly = true)
    public List<SavedAiNote> listAiNotes(String knowledgeBaseId) {
        return aiNotes.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    public void deleteAiNote(String id) {
        aiNotes.deleteById(id);
    }

    public SavedAiNote saveAiNote(SavedAiNote note) {
        return toModel(aiNotes.save(new AiNoteEntity(
            note.id(),
            note.knowledgeBaseId(),
            note.type().name(),
            note.title(),
            note.content(),
            write(note.sourceRefs()),
            note.savedAt()
        )));
    }

    @Transactional(readOnly = true)
    public Optional<TaskStatus> findTask(String id) {
        return tasks.findById(id).map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<TaskStatus> listTasks() {
        return tasks.findAll().stream().map(this::toModel).toList();
    }

    public TaskStatus saveTask(TaskStatus task) {
        return toModel(tasks.save(new TaskStatusEntity(
            task.id(),
            task.kind().name(),
            task.status().name(),
            task.knowledgeBaseId(),
            task.materialId(),
            task.generatedContentId(),
            task.providerType() == null ? null : task.providerType().name(),
            task.model(),
            task.progressPercent(),
            task.currentStep(),
            task.errorCode(),
            task.errorMessage(),
            task.retryCount(),
            task.resultRef() == null ? null : write(task.resultRef()),
            task.createdAt(),
            task.startedAt(),
            task.finishedAt(),
            task.updatedAt()
        )));
    }

    @Transactional
    public void deleteAll() {
        chunks.deleteAll();
        materials.deleteAll();
        knowledgePoints.deleteAll();
        generatedContents.deleteAll();
        questions.deleteAll();
        aiNoteDrafts.deleteAll();
        aiNotes.deleteAll();
        answerRecords.deleteAll();
        tasks.deleteAll();
        knowledgeBases.deleteAll();
    }

    private KnowledgeBase toModel(KnowledgeBaseEntity entity) {
        return new KnowledgeBase(entity.getId(), entity.getName(), entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private LearningMaterial toModel(LearningMaterialEntity entity) {
        return new LearningMaterial(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getTitle(),
            MaterialSourceType.valueOf(entity.getSourceType()),
            MaterialStatus.valueOf(entity.getStatus()),
            entity.getImportTaskId(),
            entity.getEmbeddingTaskId(),
            entity.getErrorMessage(),
            entity.getContent(),
            entity.getCreatedAt(),
            entity.getDeletedAt(),
            entity.getCurrentRevisionId()
        );
    }

    private MaterialChunk toModel(MaterialChunkEntity entity) {
        return new MaterialChunk(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getMaterialId(),
            entity.getContent(),
            entity.getOrdinal(),
            read(entity.getSourceRefJson(), SourceRef.class),
            readNullable(entity.getEmbeddingJson(), DOUBLES),
            enumOrDefault(EmbeddingStatus.class, entity.getEmbeddingStatus(), EmbeddingStatus.PENDING),
            entity.getEmbeddingModel(),
            entity.getEmbeddingDimensions()
        );
    }

    private KnowledgePoint toModel(KnowledgePointEntity entity) {
        var refs = read(entity.getSourceRefsJson(), SOURCE_REFS);
        return new KnowledgePoint(entity.getId(), entity.getKnowledgeBaseId(), entity.getName(), entity.getDescription(), entity.getSourceMaterialId(), refs,
            entity.getTitle() == null ? entity.getName() : entity.getTitle(), entity.getShortSummary() == null ? entity.getDescription() : entity.getShortSummary(), entity.getDefinition(),
            readNullable(entity.getPrinciplesJson(), STRINGS) == null ? List.of() : readNullable(entity.getPrinciplesJson(), STRINGS),
            readNullable(entity.getApplicationScenariosJson(), STRINGS) == null ? List.of() : readNullable(entity.getApplicationScenariosJson(), STRINGS),
            readNullable(entity.getPitfallsJson(), STRINGS) == null ? List.of() : readNullable(entity.getPitfallsJson(), STRINGS),
            enumOrDefault(KnowledgePointReviewStatus.class, entity.getReviewStatus(), KnowledgePointReviewStatus.CONFIRMED),
            Boolean.TRUE.equals(entity.getSourceOutdated()), Boolean.TRUE.equals(entity.getLegacy()));
    }

    private GeneratedQuestionDraft toModel(GeneratedContentEntity entity) {
        return new GeneratedQuestionDraft(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getGenerationTaskId(),
            GeneratedContentStatus.valueOf(entity.getStatus()),
            read(entity.getSourceRefsJson(), SOURCE_REFS),
            enumOrNull(SourceType.class, entity.getSourceType()),
            entity.getSourceId(),
            QuestionType.valueOf(entity.getQuestionType()),
            entity.getCategoryId(),
            entity.getCategoryName(),
            read(entity.getKnowledgePointIdsJson(), STRINGS),
            entity.getStem(),
            read(entity.getOptionsJson(), STRINGS),
            read(entity.getAnswerJson(), STRINGS),
            entity.getExplanation(),
            entity.getSavedQuestionId(),
            entity.getSavedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(), entity.getKnowledgePointId(), entity.getMaterialId(), entity.getRevisionId(), entity.getEvidenceExcerpt()
        );
    }

    private QuestionSummary toModel(QuestionEntity entity) {
        return new QuestionSummary(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            QuestionType.valueOf(entity.getQuestionType()),
            entity.getStem(),
            entity.getCategoryId(),
            entity.getCategoryName(),
            entity.getDifficulty(),
            read(entity.getKnowledgePointIdsJson(), STRINGS),
            entity.getAnsweredCount(),
            entity.getCorrectRate(),
            read(entity.getSourceRefsJson(), SOURCE_REFS),
            entity.getCreatedAt(),
            entity.getSavedAt()
        );
    }

    private AnswerRecord toModel(AnswerRecordEntity entity) {
        return new AnswerRecord(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getQuestionId(),
            read(entity.getUserAnswerJson(), STRINGS),
            entity.isCorrect(),
            entity.getDurationMs(),
            entity.getAnsweredAt()
        );
    }

    private AiNoteDraft toModel(AiNoteDraftEntity entity) {
        return new AiNoteDraft(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getGenerationTaskId(),
            AiNoteType.valueOf(entity.getType()),
            entity.getTitle(),
            entity.getContent(),
            read(entity.getSourceRefsJson(), SOURCE_REFS),
            entity.getCreatedAt()
        );
    }

    private SavedAiNote toModel(AiNoteEntity entity) {
        return new SavedAiNote(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            AiNoteType.valueOf(entity.getType()),
            entity.getTitle(),
            entity.getContent(),
            read(entity.getSourceRefsJson(), SOURCE_REFS),
            entity.getSavedAt()
        );
    }

    private TaskStatus toModel(TaskStatusEntity entity) {
        return new TaskStatus(
            entity.getId(),
            TaskKind.valueOf(entity.getKind()),
            TaskLifecycleStatus.valueOf(entity.getStatus()),
            entity.getKnowledgeBaseId(),
            entity.getMaterialId(),
            entity.getGeneratedContentId(),
            enumOrNull(AiProviderType.class, entity.getProviderType()),
            entity.getModel(),
            entity.getProgressPercent(),
            entity.getCurrentStep(),
            entity.getErrorCode(),
            entity.getErrorMessage(),
            entity.getRetryCount(),
            readNullable(entity.getResultRefJson(), TaskResultRef.class),
            entity.getCreatedAt(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getUpdatedAt()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize persistent value", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to deserialize persistent value", exception);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to deserialize persistent value", exception);
        }
    }

    private <T> T readNullable(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return read(json, type);
    }

    private <T> T readNullable(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return read(json, type);
    }

    private <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private <E extends Enum<E>> E enumOrDefault(Class<E> type, String value, E defaultValue) {
        return value == null ? defaultValue : Enum.valueOf(type, value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

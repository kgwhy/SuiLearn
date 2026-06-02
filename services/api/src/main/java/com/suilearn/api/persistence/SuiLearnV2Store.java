package com.suilearn.api.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.persistence.entity.AiNoteDraftEntity;
import com.suilearn.api.persistence.entity.AiNoteEntity;
import com.suilearn.api.persistence.entity.GeneratedContentEntity;
import com.suilearn.api.persistence.entity.KnowledgeBaseEntity;
import com.suilearn.api.persistence.entity.KnowledgePointEntity;
import com.suilearn.api.persistence.entity.LearningMaterialEntity;
import com.suilearn.api.persistence.entity.MaterialChunkEntity;
import com.suilearn.api.persistence.entity.QuestionEntity;
import com.suilearn.api.persistence.repository.AiNoteDraftJpaRepository;
import com.suilearn.api.persistence.repository.AiNoteJpaRepository;
import com.suilearn.api.persistence.repository.GeneratedContentJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgeBaseJpaRepository;
import com.suilearn.api.persistence.repository.KnowledgePointJpaRepository;
import com.suilearn.api.persistence.repository.LearningMaterialJpaRepository;
import com.suilearn.api.persistence.repository.MaterialChunkJpaRepository;
import com.suilearn.api.persistence.repository.QuestionJpaRepository;
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
    private final ObjectMapper objectMapper;

    public SuiLearnV2Store(
        KnowledgeBaseJpaRepository knowledgeBases,
        LearningMaterialJpaRepository materials,
        MaterialChunkJpaRepository chunks,
        KnowledgePointJpaRepository knowledgePoints,
        GeneratedContentJpaRepository generatedContents,
        QuestionJpaRepository questions,
        AiNoteDraftJpaRepository aiNoteDrafts,
        AiNoteJpaRepository aiNotes,
        ObjectMapper objectMapper
    ) {
        this.knowledgeBases = knowledgeBases;
        this.materials = materials;
        this.chunks = chunks;
        this.knowledgePoints = knowledgePoints;
        this.generatedContents = generatedContents;
        this.questions = questions;
        this.aiNoteDrafts = aiNoteDrafts;
        this.aiNotes = aiNotes;
        this.objectMapper = objectMapper;
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
        knowledgeBases.deleteById(knowledgeBaseId);
    }

    public List<LearningMaterial> listMaterials() {
        return materials.findAll().stream().map(this::toModel).toList();
    }

    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        return materials.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

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
            material.content(),
            material.createdAt(),
            material.deletedAt()
        )));
    }

    @Transactional
    public void saveChunks(String materialId, List<MaterialChunk> materialChunks) {
        chunks.deleteByMaterialId(materialId);
        chunks.saveAll(materialChunks.stream()
            .map(chunk -> new MaterialChunkEntity(
                chunk.id(),
                chunk.materialId(),
                chunk.content(),
                chunk.ordinal(),
                write(chunk.sourceRef()),
                write(chunk.embedding()),
                chunk.embeddingModel()
            ))
            .toList());
    }

    public List<MaterialChunk> listChunks() {
        return chunks.findAll().stream().map(this::toModel).toList();
    }

    public List<MaterialChunk> listChunksByMaterial(String materialId) {
        return chunks.findByMaterialId(materialId).stream().map(this::toModel).toList();
    }

    public Optional<MaterialChunk> findChunk(String id) {
        return chunks.findById(id).map(this::toModel);
    }

    public List<KnowledgePoint> listKnowledgePoints() {
        return knowledgePoints.findAll().stream().map(this::toModel).toList();
    }

    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        return knowledgePoints.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    public Optional<KnowledgePoint> findKnowledgePoint(String id) {
        return knowledgePoints.findById(id).map(this::toModel);
    }

    public KnowledgePoint saveKnowledgePoint(KnowledgePoint point) {
        return toModel(knowledgePoints.save(new KnowledgePointEntity(
            point.id(),
            point.knowledgeBaseId(),
            point.name(),
            point.description(),
            point.sourceMaterialId(),
            write(point.sourceRefs())
        )));
    }

    public void deleteKnowledgePoint(String id) {
        knowledgePoints.deleteById(id);
    }

    public List<GeneratedQuestionDraft> listGeneratedContents() {
        return generatedContents.findAll().stream().map(this::toModel).toList();
    }

    public Optional<GeneratedQuestionDraft> findGeneratedContent(String id) {
        return generatedContents.findById(id).map(this::toModel);
    }

    public GeneratedQuestionDraft saveGeneratedContent(GeneratedQuestionDraft draft) {
        return toModel(generatedContents.save(new GeneratedContentEntity(
            draft.id(),
            draft.knowledgeBaseId(),
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
        )));
    }

    public List<QuestionSummary> listQuestions() {
        return questions.findAll().stream().map(this::toModel).toList();
    }

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

    public List<AiNoteDraft> listAiNoteDrafts(String knowledgeBaseId) {
        return aiNoteDrafts.findByKnowledgeBaseId(knowledgeBaseId).stream().map(this::toModel).toList();
    }

    public AiNoteDraft saveAiNoteDraft(AiNoteDraft note) {
        return toModel(aiNoteDrafts.save(new AiNoteDraftEntity(
            note.id(),
            note.knowledgeBaseId(),
            note.type().name(),
            note.title(),
            note.content(),
            write(note.sourceRefs()),
            note.createdAt()
        )));
    }

    public List<SavedAiNote> listAiNotes() {
        return aiNotes.findAll().stream().map(this::toModel).toList();
    }

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

    @Transactional
    public void deleteAll() {
        chunks.deleteAll();
        materials.deleteAll();
        knowledgePoints.deleteAll();
        generatedContents.deleteAll();
        questions.deleteAll();
        aiNoteDrafts.deleteAll();
        aiNotes.deleteAll();
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
            entity.getContent(),
            entity.getCreatedAt(),
            entity.getDeletedAt()
        );
    }

    private MaterialChunk toModel(MaterialChunkEntity entity) {
        return new MaterialChunk(
            entity.getId(),
            entity.getMaterialId(),
            entity.getContent(),
            entity.getOrdinal(),
            read(entity.getSourceRefJson(), SourceRef.class),
            readNullable(entity.getEmbeddingJson(), DOUBLES),
            entity.getEmbeddingModel()
        );
    }

    private KnowledgePoint toModel(KnowledgePointEntity entity) {
        return new KnowledgePoint(
            entity.getId(),
            entity.getKnowledgeBaseId(),
            entity.getName(),
            entity.getDescription(),
            entity.getSourceMaterialId(),
            read(entity.getSourceRefsJson(), SOURCE_REFS)
        );
    }

    private GeneratedQuestionDraft toModel(GeneratedContentEntity entity) {
        return new GeneratedQuestionDraft(
            entity.getId(),
            entity.getKnowledgeBaseId(),
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
            entity.getUpdatedAt()
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

    private AiNoteDraft toModel(AiNoteDraftEntity entity) {
        return new AiNoteDraft(
            entity.getId(),
            entity.getKnowledgeBaseId(),
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

    private <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}

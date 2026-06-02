package com.suilearn.api.service;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.dto.RenameKnowledgeBaseRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.AiNoteType;
import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.KnowledgePoint;
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
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.retrieval.Retriever;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SuiLearnV2Service {
    private static final String FAKE_EMBEDDING_MODEL = "fake-embedding-v1";

    private final AiProvider aiProvider;
    private final Clock clock;
    private final EmbeddingProvider embeddingProvider;
    private final MaterialChunker materialChunker;
    private final MaterialParser materialParser;
    private final Retriever retriever;
    private final SuiLearnV2Store store;

    public SuiLearnV2Service(
        AiProvider aiProvider,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Retriever retriever,
        Clock clock,
        SuiLearnV2Store store
    ) {
        this.aiProvider = aiProvider;
        this.clock = clock;
        this.embeddingProvider = embeddingProvider;
        this.materialChunker = materialChunker;
        this.materialParser = materialParser;
        this.retriever = retriever;
        this.store = store;
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

    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return store.listMaterials(knowledgeBaseId).stream()
            .sorted(Comparator.comparing(LearningMaterial::createdAt))
            .toList();
    }

    public LearningMaterial importMaterial(String knowledgeBaseId, ImportMaterialRequest request) {
        requireKnowledgeBase(knowledgeBaseId);
        var material = new LearningMaterial(
            newId("mat"),
            knowledgeBaseId,
            request.title(),
            request.sourceType(),
            MaterialStatus.UPLOADED,
            request.content(),
            clock.instant(),
            null
        );
        var saved = store.saveMaterial(material);
        try {
            var parsing = store.saveMaterial(withStatus(saved, MaterialStatus.PARSING));
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
            var chunks = materialChunker.chunk(chunking);
            var indexing = store.saveMaterial(withStatus(chunking, MaterialStatus.INDEXING));
            store.saveChunks(indexing.id(), chunks.stream().map(this::withEmbedding).toList());
            return store.saveMaterial(withStatus(indexing, MaterialStatus.READY));
        } catch (RuntimeException exception) {
            return store.saveMaterial(withStatus(saved, MaterialStatus.FAILED));
        }
    }

    public MaterialDetail getMaterialDetail(String materialId) {
        var material = requireMaterial(materialId);
        return new MaterialDetail(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            material.status(),
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
            material.content(),
            material.createdAt(),
            deletedAt
        );
        store.saveMaterial(updatedMaterial);

        var invalidatedChunkCount = store.listChunksByMaterial(materialId).size();
        var deletedPendingCount = 0;
        for (var content : store.listGeneratedContents()) {
            if (!referencesMaterial(content.sourceRefs(), materialId)) {
                continue;
            }
            if (content.status() == GeneratedContentStatus.PENDING_REVIEW
                && effectivePendingPolicy == DeletedMaterialPendingContentPolicy.DELETE_PENDING_GENERATED_CONTENT) {
                store.saveGeneratedContent(updateGeneratedStatus(content, GeneratedContentStatus.DELETED));
                deletedPendingCount++;
            } else {
                store.saveGeneratedContent(withSourceRefs(content, markSourceDeleted(content.sourceRefs(), materialId)));
            }
        }

        var retainedSavedQuestionCount = 0;
        for (var question : store.listQuestions()) {
            if (!referencesMaterial(question.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                store.deleteQuestion(question.id());
            } else {
                store.saveQuestion(withSourceRefs(question, markSourceDeleted(question.sourceRefs(), materialId)));
                retainedSavedQuestionCount++;
            }
        }

        var retainedAiNoteCount = 0;
        for (var note : store.listAiNotes()) {
            if (!referencesMaterial(note.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                store.deleteAiNote(note.id());
            } else {
                store.saveAiNote(withSourceRefs(note, markSourceDeleted(note.sourceRefs(), materialId)));
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

    public List<KnowledgePoint> extractKnowledgePoints(String materialId) {
        var material = requireMaterial(materialId);
        var candidates = extractCandidateTerms(material.content());
        var extracted = candidates.stream()
            .map(term -> new KnowledgePoint(
                newId("kp"),
                material.knowledgeBaseId(),
                term,
                "从资料《" + material.title() + "》中提取的候选知识点。",
                material.id(),
                List.of(materialSourceRef(material))
            ))
            .toList();
        extracted.forEach(store::saveKnowledgePoint);
        return extracted;
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
        var sourceRefs = normalizeSourceRefs(request.knowledgeBaseId(), request.sourceRefs());
        ensureSourcesUsableForGeneration(sourceRefs);
        var now = clock.instant();
        var type = request.questionType() == null ? QuestionType.SINGLE_CHOICE : request.questionType();
        var draftKnowledgePointIds = requestedKnowledgePointIds(request.knowledgePointIds(), sourceRefs);
        var categoryId = valueOrDefault(request.categoryId(), defaultCategoryId(draftKnowledgePointIds));
        var categoryName = valueOrDefault(request.categoryName(), defaultCategoryName(categoryId));
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
        return store.saveGeneratedContent(draft);
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
        return new KnowledgeBaseStatistics(
            knowledgeBaseId,
            questionCount,
            0,
            0,
            null,
            0,
            store.listKnowledgePoints(knowledgeBaseId).stream()
                .map(KnowledgePoint::id)
                .limit(3)
                .toList()
        );
    }

    public AiNoteDraft generateExplanation(GenerateExplanationRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var point = requireKnowledgePoint(request.knowledgePointId());
        if (!point.knowledgeBaseId().equals(request.knowledgeBaseId())) {
            throw new IllegalArgumentException("Knowledge point is outside knowledge base: " + request.knowledgePointId());
        }
        var sourceRefs = normalizeSourceRefs(request.knowledgeBaseId(), request.sourceRefs());
        ensureSourcesUsableForGeneration(sourceRefs);
        var generated = aiProvider.generateKnowledgePointExplanation(new AiProvider.KnowledgePointExplanationPrompt(
            request.knowledgeBaseId(),
            point.id(),
            point.name(),
            point.description(),
            sourceRefs,
            request.prompt()
        ));
        var draft = new AiNoteDraft(
            newId("note_draft"),
            request.knowledgeBaseId(),
            AiNoteType.KNOWLEDGE_POINT_EXPLANATION,
            requireGeneratedText(generated.title(), "explanation title"),
            requireGeneratedText(generated.content(), "explanation content"),
            sourceRefs,
            clock.instant()
        );
        return store.saveAiNoteDraft(draft);
    }

    public AiNoteDraft generateReviewSuggestion(GenerateReviewSuggestionRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var sourceRefs = normalizeSourceRefs(request.knowledgeBaseId(), request.sourceRefs());
        ensureSourcesUsableForGeneration(sourceRefs);
        var weakPoints = request.weakKnowledgePointIds() == null ? List.<String>of() : request.weakKnowledgePointIds();
        var generated = aiProvider.generateReviewSuggestion(new AiProvider.ReviewSuggestionPrompt(
            request.knowledgeBaseId(),
            sourceRefs,
            weakPoints,
            request.wrongQuestionIds() == null ? List.of() : request.wrongQuestionIds(),
            request.prompt()
        ));
        var title = weakPoints.isEmpty() ? "Review suggestion" : "Weak knowledge point review suggestion";
        var draft = new AiNoteDraft(
            newId("note_draft"),
            request.knowledgeBaseId(),
            AiNoteType.REVIEW_SUGGESTION,
            requireGeneratedText(generated.title(), "review suggestion title"),
            requireGeneratedText(generated.content(), "review suggestion content"),
            sourceRefs,
            clock.instant()
        );
        return store.saveAiNoteDraft(draft);
    }

    public SavedAiNote saveAiNote(SaveAiNoteRequest request) {
        requireKnowledgeBase(request.knowledgeBaseId());
        var sourceRefs = normalizeSourceRefs(request.knowledgeBaseId(), request.sourceRefs());
        ensureSourcesUsableForGeneration(sourceRefs);
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
            : normalizeSourceRefs(existing.knowledgeBaseId(), request.sourceRefs());
        var savedQuestionId = existing.savedQuestionId();
        var savedAt = existing.savedAt();
        if (request.status() == GeneratedContentStatus.SAVED && savedQuestionId == null) {
            ensureSourcesUsableForGeneration(sourceRefs);
            savedQuestionId = newId("q");
            savedAt = clock.instant();
        } else if (request.status() == GeneratedContentStatus.SAVED) {
            ensureSourcesUsableForGeneration(sourceRefs);
        }
        var reviewKnowledgePointIds = request.knowledgePointIds() == null || request.knowledgePointIds().isEmpty()
            ? existing.knowledgePointIds()
            : request.knowledgePointIds().stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        var categoryId = valueOrDefault(request.categoryId(), existing.categoryId());
        var categoryName = valueOrDefault(request.categoryName(), existing.categoryName());
        var updated = new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
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
        var scope = requireSearchScope(knowledgeBaseId, materialId);
        return retriever.search(new Retriever.RetrievalRequest(query, scope.knowledgeBaseId(), scope.materialId()));
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

    private LearningMaterial withContentAndStatus(LearningMaterial material, String content, MaterialStatus status) {
        return new LearningMaterial(
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.sourceType(),
            status,
            content,
            material.createdAt(),
            material.deletedAt()
        );
    }

    private MaterialChunk withEmbedding(MaterialChunk chunk) {
        return new MaterialChunk(
            chunk.id(),
            chunk.materialId(),
            chunk.content(),
            chunk.ordinal(),
            chunk.sourceRef(),
            embeddingProvider.embed(chunk.content()).values(),
            FAKE_EMBEDDING_MODEL
        );
    }

    private List<String> extractCandidateTerms(String content) {
        return List.of(content.split("[,，。；;、\\s]+")).stream()
            .map(String::trim)
            .filter(term -> term.length() >= 2 && term.length() <= 32)
            .distinct()
            .limit(8)
            .toList();
    }

    private SourceRef materialSourceRef(LearningMaterial material) {
        return new SourceRef(
            SourceType.MATERIAL,
            material.id(),
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            null,
            material.status() == MaterialStatus.DELETED,
            null
        );
    }

    private SourceRef chunkSourceRef(LearningMaterial material, String chunkId, String content) {
        return new SourceRef(
            SourceType.MATERIAL_CHUNK,
            chunkId,
            material.knowledgeBaseId(),
            material.title(),
            material.id(),
            chunkId,
            material.status() == MaterialStatus.DELETED,
            truncate(content)
        );
    }

    private List<SourceRef> normalizeSourceRefs(String knowledgeBaseId, List<SourceRef> sourceRefs) {
        if (sourceRefs == null || sourceRefs.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceRef is required");
        }
        return sourceRefs.stream()
            .map(ref -> normalizeSourceRef(knowledgeBaseId, ref))
            .toList();
    }

    private SourceRef normalizeSourceRef(String knowledgeBaseId, SourceRef ref) {
        if (ref == null || ref.type() == null || ref.id() == null || ref.id().isBlank()) {
            throw new IllegalArgumentException("Invalid sourceRef");
        }
        if (ref.type() == SourceType.KNOWLEDGE_BASE) {
            requireKnowledgeBase(ref.id());
            if (!ref.id().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            var kb = requireKnowledgeBase(ref.id());
            return new SourceRef(ref.type(), ref.id(), kb.id(), kb.name(), null, null, false, ref.excerpt());
        }
        if (ref.type() == SourceType.KNOWLEDGE_POINT) {
            var point = requireKnowledgePoint(ref.id());
            if (!point.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return new SourceRef(
                ref.type(),
                point.id(),
                point.knowledgeBaseId(),
                point.name(),
                point.sourceMaterialId(),
                null,
                isMaterialDeleted(point.sourceMaterialId()),
                ref.excerpt()
            );
        }
        if (ref.type() == SourceType.MATERIAL) {
            var material = requireMaterial(ref.id());
            if (!material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return materialSourceRef(material);
        }
        if (ref.type() == SourceType.MATERIAL_CHUNK) {
            var chunk = requireChunk(ref.id());
            var material = requireMaterial(chunk.materialId());
            if (!material.knowledgeBaseId().equals(knowledgeBaseId)) {
                throw new IllegalArgumentException("SourceRef is outside knowledge base: " + ref.id());
            }
            return chunkSourceRef(material, chunk.id(), chunk.content());
        }
        return new SourceRef(
            ref.type(),
            ref.id(),
            valueOrDefault(ref.knowledgeBaseId(), knowledgeBaseId),
            ref.title(),
            ref.materialId(),
            ref.chunkId(),
            ref.deleted(),
            ref.excerpt()
        );
    }

    private void ensureSourcesUsableForGeneration(List<SourceRef> sourceRefs) {
        for (var ref : sourceRefs) {
            if (ref.deleted()) {
                throw new IllegalArgumentException("SourceRef is deleted: " + ref.id());
            }
        }
    }

    private MaterialChunk requireChunk(String chunkId) {
        return store.findChunk(chunkId)
            .orElseThrow(() -> new IllegalArgumentException("Material chunk not found: " + chunkId));
    }

    private GeneratedQuestionDraft updateGeneratedStatus(GeneratedQuestionDraft existing, GeneratedContentStatus status) {
        return new GeneratedQuestionDraft(
            existing.id(),
            existing.knowledgeBaseId(),
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

    private boolean referencesMaterial(List<SourceRef> refs, String materialId) {
        return refs != null && refs.stream().anyMatch(ref -> referencesMaterial(ref, materialId));
    }

    private boolean referencesMaterial(SourceRef ref, String materialId) {
        return ref != null
            && (materialId.equals(ref.materialId())
                || (ref.type() == SourceType.MATERIAL && materialId.equals(ref.id())));
    }

    private boolean isMaterialDeleted(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return false;
        }
        return store.findMaterial(materialId)
            .map(material -> material.status() == MaterialStatus.DELETED)
            .orElse(false);
    }

    private List<SourceRef> markSourceDeleted(List<SourceRef> refs, String materialId) {
        return refs.stream()
            .map(ref -> referencesMaterial(ref, materialId)
                ? new SourceRef(ref.type(), ref.id(), ref.knowledgeBaseId(), ref.title(), ref.materialId(), ref.chunkId(), true, ref.excerpt())
                : ref)
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

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

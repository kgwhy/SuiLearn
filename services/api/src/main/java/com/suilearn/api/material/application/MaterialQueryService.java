package com.suilearn.api.material.application;

import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialDeletionResult;
import com.suilearn.api.model.MaterialDetail;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.generation.infrastructure.AiNoteStore;
import com.suilearn.api.generation.infrastructure.GeneratedContentStore;
import com.suilearn.api.generation.infrastructure.QuestionStore;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.source.application.SourceService;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MaterialQueryService {
    private final AiNoteStore aiNotes;
    private final Clock clock;
    private final GeneratedContentStore generatedContents;
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialChunkStore materialChunks;
    private final MaterialStore materials;
    private final QuestionStore questions;
    private final SourceService sourceService;

    public MaterialQueryService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        MaterialChunkStore materialChunks,
        KnowledgePointStore knowledgePoints,
        GeneratedContentStore generatedContents,
        QuestionStore questions,
        AiNoteStore aiNotes,
        SourceService sourceService,
        Clock clock
    ) {
        this.aiNotes = aiNotes;
        this.clock = clock;
        this.generatedContents = generatedContents;
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materialChunks = materialChunks;
        this.materials = materials;
        this.questions = questions;
        this.sourceService = sourceService;
    }

    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return materials.list(knowledgeBaseId).stream()
            .filter(material -> material.status() != MaterialStatus.DELETED)
            .sorted(Comparator.comparing(LearningMaterial::createdAt))
            .toList();
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
            materialChunks.listByMaterial(material.id()),
            knowledgePoints.list().stream()
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
        materials.save(new LearningMaterial(
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
        ));

        var invalidatedChunkCount = materialChunks.invalidateByMaterial(materialId);
        var deletedPendingCount = 0;
        for (var content : generatedContents.list()) {
            if (!sourceService.referencesMaterial(content.sourceRefs(), materialId)) {
                continue;
            }
            if (content.status() == GeneratedContentStatus.PENDING_REVIEW
                && effectivePendingPolicy == DeletedMaterialPendingContentPolicy.DELETE_PENDING_GENERATED_CONTENT) {
                generatedContents.save(updateGeneratedStatus(content, GeneratedContentStatus.DELETED));
                deletedPendingCount++;
            } else {
                generatedContents.save(withSourceRefs(content, sourceService.markMaterialDeleted(content.sourceRefs(), materialId)));
            }
        }

        var retainedSavedQuestionCount = 0;
        for (var question : questions.list()) {
            if (!sourceService.referencesMaterial(question.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                questions.delete(question.id());
            } else {
                questions.save(withSourceRefs(question, sourceService.markMaterialDeleted(question.sourceRefs(), materialId)));
                retainedSavedQuestionCount++;
            }
        }

        var retainedAiNoteCount = 0;
        for (var note : aiNotes.listSaved()) {
            if (!sourceService.referencesMaterial(note.sourceRefs(), materialId)) {
                continue;
            }
            if (effectiveSavedPolicy == DeletedMaterialSavedContentPolicy.DELETE_SAVED_CONTENT) {
                aiNotes.delete(note.id());
            } else {
                aiNotes.save(withSourceRefs(note, sourceService.markMaterialDeleted(note.sourceRefs(), materialId)));
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

    private void requireKnowledgeBase(String knowledgeBaseId) {
        knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private LearningMaterial requireMaterial(String materialId) {
        return materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
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

    private String truncate(String value) {
        if (value == null || value.length() <= 160) {
            return value;
        }
        return value.substring(0, 160);
    }
}

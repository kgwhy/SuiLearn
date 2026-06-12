package com.suilearn.api.knowledgebase.application;

import com.suilearn.api.dto.CreateKnowledgeBaseRequest;
import com.suilearn.api.dto.RenameKnowledgeBaseRequest;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.generation.infrastructure.AiNoteStore;
import com.suilearn.api.generation.infrastructure.GeneratedContentStore;
import com.suilearn.api.generation.infrastructure.QuestionStore;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {
    private final AiNoteStore aiNotes;
    private final Clock clock;
    private final GeneratedContentStore generatedContents;
    private final KnowledgeBaseStore knowledgeBases;
    private final KnowledgePointStore knowledgePoints;
    private final MaterialStore materials;
    private final QuestionStore questions;

    public KnowledgeBaseService(
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        KnowledgePointStore knowledgePoints,
        QuestionStore questions,
        GeneratedContentStore generatedContents,
        AiNoteStore aiNotes,
        Clock clock
    ) {
        this.aiNotes = aiNotes;
        this.clock = clock;
        this.generatedContents = generatedContents;
        this.knowledgeBases = knowledgeBases;
        this.knowledgePoints = knowledgePoints;
        this.materials = materials;
        this.questions = questions;
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBases.list().stream()
            .sorted(Comparator.comparing(KnowledgeBase::createdAt))
            .toList();
    }

    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        var now = clock.instant();
        return knowledgeBases.save(new KnowledgeBase(newId("kb"), request.name(), request.description(), now, now));
    }

    public KnowledgeBaseDetail getKnowledgeBaseDetail(String knowledgeBaseId) {
        var knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        return new KnowledgeBaseDetail(
            knowledgeBase.id(),
            knowledgeBase.name(),
            knowledgeBase.description(),
            knowledgeBase.createdAt(),
            knowledgeBase.updatedAt(),
            materials.list(knowledgeBaseId).size(),
            knowledgePoints.list(knowledgeBaseId).size(),
            questions.list(knowledgeBaseId).size(),
            (int) generatedContents.list().stream()
                .filter(content -> content.knowledgeBaseId().equals(knowledgeBaseId))
                .filter(content -> content.status() != GeneratedContentStatus.DELETED)
                .count(),
            aiNotes.listSaved(knowledgeBaseId).size()
        );
    }

    public KnowledgeBase renameKnowledgeBase(String knowledgeBaseId, RenameKnowledgeBaseRequest request) {
        var existing = requireKnowledgeBase(knowledgeBaseId);
        return knowledgeBases.save(new KnowledgeBase(
            existing.id(),
            request.name(),
            request.description(),
            existing.createdAt(),
            clock.instant()
        ));
    }

    public void deleteKnowledgeBase(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        knowledgeBases.delete(knowledgeBaseId);
    }

    public List<QuestionSummary> listQuestions(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return questions.list(knowledgeBaseId);
    }

    public KnowledgeBaseStatistics getStatistics(String knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        var questionCount = questions.list(knowledgeBaseId).size();
        var materials = this.materials.list(knowledgeBaseId).stream()
            .filter(material -> material.status() != MaterialStatus.DELETED)
            .toList();
        var generated = generatedContents.list().stream()
            .filter(content -> content.knowledgeBaseId().equals(knowledgeBaseId))
            .toList();
        return new KnowledgeBaseStatistics(
            knowledgeBaseId,
            questionCount,
            materials.size(),
            (int) materials.stream().filter(material -> material.status() == MaterialStatus.READY).count(),
            knowledgePoints.list(knowledgeBaseId).size(),
            (int) generated.stream().filter(content -> content.status() == GeneratedContentStatus.PENDING_REVIEW).count(),
            aiNotes.listSaved(knowledgeBaseId).size(),
            0,
            0,
            null,
            0,
            knowledgePoints.list(knowledgeBaseId).stream()
                .map(point -> point.id())
                .limit(3)
                .toList()
        );
    }

    private KnowledgeBase requireKnowledgeBase(String knowledgeBaseId) {
        return knowledgeBases.find(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

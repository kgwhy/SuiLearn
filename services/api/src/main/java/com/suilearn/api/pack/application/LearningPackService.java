package com.suilearn.api.pack.application;

import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.model.KnowledgeBase;
import org.springframework.stereotype.Service;

@Service
public class LearningPackService {
    private final KnowledgeBaseService knowledgeBaseService;

    public LearningPackService(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public LearningPack resolve(String packId) {
        var knowledgeBase = knowledgeBaseService.listKnowledgeBases().stream()
            .filter(candidate -> candidate.id().equals(packId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Learning pack not found: " + packId));
        return toLearningPack(knowledgeBase);
    }

    private LearningPack toLearningPack(KnowledgeBase knowledgeBase) {
        return new LearningPack(
            knowledgeBase.id(),
            knowledgeBase.id(),
            knowledgeBase.name(),
            knowledgeBase.description()
        );
    }

    public record LearningPack(String id, String knowledgeBaseId, String name, String description) {
    }
}

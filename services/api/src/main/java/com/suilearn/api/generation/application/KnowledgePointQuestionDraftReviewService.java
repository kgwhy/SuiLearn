package com.suilearn.api.generation.application;

import com.suilearn.api.dto.ReviewKnowledgePointQuestionDraftRequest;
import com.suilearn.api.model.GeneratedQuestionDraft;
import org.springframework.stereotype.Service;

/** Closed review boundary for drafts whose knowledge-point attribution and evidence are immutable. */
@Service
public class KnowledgePointQuestionDraftReviewService {
    private final GeneratedContentService generatedContents;

    public KnowledgePointQuestionDraftReviewService(GeneratedContentService generatedContents) {
        this.generatedContents = generatedContents;
    }

    public GeneratedQuestionDraft review(String generatedContentId, ReviewKnowledgePointQuestionDraftRequest request) {
        return generatedContents.reviewKnowledgePointQuestionDraft(generatedContentId, request);
    }
}

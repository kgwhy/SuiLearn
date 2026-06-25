package com.suilearn.api.rag.application;

import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.RagAnswer;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.retrieval.Retriever;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RagService {
    private final AiProvider aiProvider;
    private final KnowledgeBaseStore knowledgeBases;
    private final MaterialStore materials;
    private final Retriever retriever;

    public RagService(
        AiProvider aiProvider,
        KnowledgeBaseStore knowledgeBases,
        MaterialStore materials,
        Retriever retriever
    ) {
        this.aiProvider = aiProvider;
        this.knowledgeBases = knowledgeBases;
        this.materials = materials;
        this.retriever = retriever;
    }

    public RagAnswer ask(String question, String knowledgeBaseId, String materialId) {
        if (isBlank(knowledgeBaseId) && isBlank(materialId)) {
            throw new IllegalArgumentException("At least one scope is required: knowledgeBaseId or materialId");
        }
        var scopedKnowledgeBaseId = knowledgeBaseId;
        if (!isBlank(materialId)) {
            var material = materials.find(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
            if (material.status() == MaterialStatus.DELETED) {
                return new RagAnswer("不确定：资料已删除，无法作为回答依据。", true, List.of(), List.of(), null);
            }
            if (!isBlank(scopedKnowledgeBaseId) && !material.knowledgeBaseId().equals(scopedKnowledgeBaseId)) {
                throw new IllegalArgumentException("Material is outside knowledge base: " + materialId);
            }
            scopedKnowledgeBaseId = material.knowledgeBaseId();
        } else if (!isBlank(scopedKnowledgeBaseId)) {
            var requestedKnowledgeBaseId = scopedKnowledgeBaseId;
            knowledgeBases.find(requestedKnowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + requestedKnowledgeBaseId));
        }
        var citations = retriever.retrieveEvidence(
            new Retriever.RetrievalRequest(question, scopedKnowledgeBaseId, materialId),
            3
        );
        if (citations.isEmpty()) {
            return new RagAnswer("不确定：资料中未找到明确依据。", true, List.of(), List.of(), null);
        }
        var sourceRefs = citations.stream().map(MaterialChunk::sourceRef).toList();
        var generated = aiProvider.answerQuestion(new AiProvider.AnswerQuestionPrompt(
            scopedKnowledgeBaseId,
            materialId,
            question,
            sourceRefs
        ));
        return new RagAnswer(
            generated.answer(),
            generated.uncertain(),
            sourceRefs,
            citations,
            null
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

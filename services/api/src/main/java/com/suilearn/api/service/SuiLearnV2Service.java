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
import com.suilearn.api.dto.SubmitAnswerRequest;
import com.suilearn.api.dto.UpdateKnowledgePointRequest;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.AnswerRecord;
import com.suilearn.api.model.DeletedMaterialPendingContentPolicy;
import com.suilearn.api.model.DeletedMaterialSavedContentPolicy;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.model.KnowledgeBaseDetail;
import com.suilearn.api.model.KnowledgeBaseStatistics;
import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.model.KnowledgePointExtractionResult;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialDeletionResult;
import com.suilearn.api.model.MaterialDetail;
import com.suilearn.api.model.QuestionSummary;
import com.suilearn.api.model.RagAnswer;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.SuiLearnV2Store;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.retrieval.Retriever;
import com.suilearn.api.service.internal.SuiLearnV2Workflow;
import com.suilearn.api.source.application.SourceService;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import com.suilearn.api.task.infrastructure.TaskStore;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SuiLearnV2Service {
    private final SuiLearnV2Workflow workflow;

    @Autowired
    public SuiLearnV2Service(SuiLearnV2Workflow workflow) {
        this.workflow = workflow;
    }

    public SuiLearnV2Service(
        AiProvider aiProvider,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Retriever retriever,
        Clock clock,
        SuiLearnV2Store store
    ) {
        this(createWorkflow(aiProvider, materialParser, materialChunker, embeddingProvider, retriever, clock, store));
    }

    private static SuiLearnV2Workflow createWorkflow(
        AiProvider aiProvider,
        MaterialParser materialParser,
        MaterialChunker materialChunker,
        EmbeddingProvider embeddingProvider,
        Retriever retriever,
        Clock clock,
        SuiLearnV2Store store
    ) {
        var taskService = new TaskService(new TaskStore(store), clock);
        return new SuiLearnV2Workflow(
            aiProvider,
            materialParser,
            materialChunker,
            embeddingProvider,
            retriever,
            clock,
            store,
            taskService,
            new TaskExecutor(taskService),
            new SourceService(
                new KnowledgeBaseStore(store),
                new KnowledgePointStore(store),
                new MaterialStore(store),
                new MaterialChunkStore(store)
            )
        );
    }

    public List<KnowledgeBase> listKnowledgeBases() {
        return workflow.listKnowledgeBases();
    }

    public KnowledgeBase createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        return workflow.createKnowledgeBase(request);
    }

    public KnowledgeBaseDetail getKnowledgeBaseDetail(String knowledgeBaseId) {
        return workflow.getKnowledgeBaseDetail(knowledgeBaseId);
    }

    public KnowledgeBase renameKnowledgeBase(String knowledgeBaseId, RenameKnowledgeBaseRequest request) {
        return workflow.renameKnowledgeBase(knowledgeBaseId, request);
    }

    public void deleteKnowledgeBase(String knowledgeBaseId) {
        workflow.deleteKnowledgeBase(knowledgeBaseId);
    }

    public TaskStatus getTaskStatus(String taskId) {
        return workflow.getTaskStatus(taskId);
    }

    public List<LearningMaterial> listMaterials(String knowledgeBaseId) {
        return workflow.listMaterials(knowledgeBaseId);
    }

    public LearningMaterial importMaterial(String knowledgeBaseId, ImportMaterialRequest request) {
        return workflow.importMaterial(knowledgeBaseId, request);
    }

    public MaterialDetail getMaterialDetail(String materialId) {
        return workflow.getMaterialDetail(materialId);
    }

    public MaterialDeletionResult deleteMaterial(
        String materialId,
        DeletedMaterialSavedContentPolicy savedContentPolicy,
        DeletedMaterialPendingContentPolicy pendingContentPolicy
    ) {
        return workflow.deleteMaterial(materialId, savedContentPolicy, pendingContentPolicy);
    }

    public KnowledgePointExtractionResult extractKnowledgePoints(String materialId) {
        return workflow.extractKnowledgePoints(materialId);
    }

    public List<KnowledgePoint> listKnowledgePoints(String knowledgeBaseId) {
        return workflow.listKnowledgePoints(knowledgeBaseId);
    }

    public KnowledgePoint updateKnowledgePoint(String knowledgePointId, UpdateKnowledgePointRequest request) {
        return workflow.updateKnowledgePoint(knowledgePointId, request);
    }

    public void deleteKnowledgePoint(String knowledgePointId) {
        workflow.deleteKnowledgePoint(knowledgePointId);
    }

    public GeneratedQuestionDraft generateQuestion(GenerateQuestionRequest request) {
        return workflow.generateQuestion(request);
    }

    public List<GeneratedQuestionDraft> listGeneratedContents(GeneratedContentStatus status) {
        return workflow.listGeneratedContents(status);
    }

    public GeneratedQuestionDraft reviewGeneratedContent(
        String generatedContentId,
        ReviewGeneratedContentRequest request
    ) {
        return workflow.reviewGeneratedContent(generatedContentId, request);
    }

    public void deleteGeneratedContent(String generatedContentId) {
        workflow.deleteGeneratedContent(generatedContentId);
    }

    public AiNoteDraft generateExplanation(GenerateExplanationRequest request) {
        return workflow.generateExplanation(request);
    }

    public AiNoteDraft generateReviewSuggestion(GenerateReviewSuggestionRequest request) {
        return workflow.generateReviewSuggestion(request);
    }

    public SavedAiNote saveAiNote(SaveAiNoteRequest request) {
        return workflow.saveAiNote(request);
    }

    public List<QuestionSummary> listQuestions(String knowledgeBaseId) {
        return workflow.listQuestions(knowledgeBaseId);
    }

    public KnowledgeBaseStatistics getStatistics(String knowledgeBaseId) {
        return workflow.getStatistics(knowledgeBaseId);
    }

    public AnswerRecord submitAnswer(String knowledgeBaseId, SubmitAnswerRequest request) {
        return workflow.submitAnswer(knowledgeBaseId, request);
    }

    public List<SearchResult> search(String query, String knowledgeBaseId, String materialId) {
        return workflow.search(query, knowledgeBaseId, materialId);
    }

    public List<SearchResult> search(String query, String knowledgeBaseId, String materialId, Integer limit) {
        return workflow.search(query, knowledgeBaseId, materialId, limit);
    }

    public RagAnswer ask(String question, String knowledgeBaseId, String materialId) {
        return workflow.ask(question, knowledgeBaseId, materialId);
    }
}

package com.suilearn.api.controller;

import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.generation.application.GeneratedContentService;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.SavedAiNote;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/ai")
public class AiGenerationController {
    private final GeneratedContentService generatedContentService;

    public AiGenerationController(GeneratedContentService generatedContentService) {
        this.generatedContentService = generatedContentService;
    }

    @PostMapping("/generated-questions")
    GeneratedQuestionDraft generateQuestion(@Valid @RequestBody GenerateQuestionRequest request) {
        return generatedContentService.generateQuestion(request);
    }

    @PostMapping("/knowledge-point-explanations")
    AiNoteDraft generateExplanation(@Valid @RequestBody GenerateExplanationRequest request) {
        return generatedContentService.generateExplanation(request);
    }

    @PostMapping("/review-suggestions")
    AiNoteDraft generateReviewSuggestion(@Valid @RequestBody GenerateReviewSuggestionRequest request) {
        return generatedContentService.generateReviewSuggestion(request);
    }

    @PostMapping("/notes")
    SavedAiNote saveAiNote(@Valid @RequestBody SaveAiNoteRequest request) {
        return generatedContentService.saveAiNote(request);
    }

    @GetMapping("/generated-contents")
    List<GeneratedQuestionDraft> listGeneratedContents(
        @RequestParam(required = false) GeneratedContentStatus status
    ) {
        return generatedContentService.listGeneratedContents(status);
    }

    @PatchMapping("/generated-contents/{generatedContentId}")
    GeneratedQuestionDraft reviewGeneratedContent(
        @PathVariable String generatedContentId,
        @Valid @RequestBody ReviewGeneratedContentRequest request
    ) {
        return generatedContentService.reviewGeneratedContent(generatedContentId, request);
    }

    @DeleteMapping("/generated-contents/{generatedContentId}")
    ResponseEntity<Void> deleteGeneratedContent(@PathVariable String generatedContentId) {
        generatedContentService.deleteGeneratedContent(generatedContentId);
        return ResponseEntity.noContent().build();
    }
}

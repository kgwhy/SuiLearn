package com.suilearn.api.controller;

import com.suilearn.api.dto.GenerateExplanationRequest;
import com.suilearn.api.dto.GenerateQuestionRequest;
import com.suilearn.api.dto.GenerateReviewSuggestionRequest;
import com.suilearn.api.dto.ReviewGeneratedContentRequest;
import com.suilearn.api.dto.SaveAiNoteRequest;
import com.suilearn.api.model.AiNoteDraft;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.SavedAiNote;
import com.suilearn.api.service.SuiLearnV2Service;
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
    private final SuiLearnV2Service service;

    public AiGenerationController(SuiLearnV2Service service) {
        this.service = service;
    }

    @PostMapping("/generated-questions")
    GeneratedQuestionDraft generateQuestion(@Valid @RequestBody GenerateQuestionRequest request) {
        return service.generateQuestion(request);
    }

    @PostMapping("/knowledge-point-explanations")
    AiNoteDraft generateExplanation(@Valid @RequestBody GenerateExplanationRequest request) {
        return service.generateExplanation(request);
    }

    @PostMapping("/review-suggestions")
    AiNoteDraft generateReviewSuggestion(@Valid @RequestBody GenerateReviewSuggestionRequest request) {
        return service.generateReviewSuggestion(request);
    }

    @PostMapping("/notes")
    SavedAiNote saveAiNote(@Valid @RequestBody SaveAiNoteRequest request) {
        return service.saveAiNote(request);
    }

    @GetMapping("/generated-contents")
    List<GeneratedQuestionDraft> listGeneratedContents(
        @RequestParam(required = false) GeneratedContentStatus status
    ) {
        return service.listGeneratedContents(status);
    }

    @PatchMapping("/generated-contents/{generatedContentId}")
    GeneratedQuestionDraft reviewGeneratedContent(
        @PathVariable String generatedContentId,
        @Valid @RequestBody ReviewGeneratedContentRequest request
    ) {
        return service.reviewGeneratedContent(generatedContentId, request);
    }

    @DeleteMapping("/generated-contents/{generatedContentId}")
    ResponseEntity<Void> deleteGeneratedContent(@PathVariable String generatedContentId) {
        service.deleteGeneratedContent(generatedContentId);
        return ResponseEntity.noContent().build();
    }
}

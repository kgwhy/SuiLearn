package com.suilearn.api.controller;

import com.suilearn.api.dto.AskQuestionRequest;
import com.suilearn.api.model.RagAnswer;
import com.suilearn.api.rag.application.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    RagAnswer ask(@Valid @RequestBody AskQuestionRequest request) {
        return ragService.ask(request.question(), request.knowledgeBaseId(), request.materialId());
    }
}

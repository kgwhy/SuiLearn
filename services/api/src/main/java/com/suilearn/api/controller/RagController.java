package com.suilearn.api.controller;

import com.suilearn.api.dto.AskQuestionRequest;
import com.suilearn.api.model.RagAnswer;
import com.suilearn.api.service.SuiLearnV2Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/rag")
public class RagController {
    private final SuiLearnV2Service service;

    public RagController(SuiLearnV2Service service) {
        this.service = service;
    }

    @PostMapping("/ask")
    RagAnswer ask(@Valid @RequestBody AskQuestionRequest request) {
        return service.ask(request.question(), request.knowledgeBaseId(), request.materialId());
    }
}

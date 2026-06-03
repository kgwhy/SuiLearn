package com.suilearn.api.controller;

import com.suilearn.api.model.AiProviderStatus;
import com.suilearn.api.service.AiProviderStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/ai")
public class AiProviderController {
    private final AiProviderStatusService service;

    public AiProviderController(AiProviderStatusService service) {
        this.service = service;
    }

    @GetMapping("/provider-status")
    AiProviderStatus getProviderStatus() {
        return service.getStatus();
    }
}

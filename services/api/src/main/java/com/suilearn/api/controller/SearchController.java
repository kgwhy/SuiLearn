package com.suilearn.api.controller;

import com.suilearn.api.model.SearchResult;
import com.suilearn.api.service.SuiLearnV2Service;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/search")
public class SearchController {
    private final SuiLearnV2Service service;

    public SearchController(SuiLearnV2Service service) {
        this.service = service;
    }

    @GetMapping
    List<SearchResult> search(
        @RequestParam("q") String query,
        @RequestParam(required = false) String knowledgeBaseId,
        @RequestParam(required = false) String materialId
    ) {
        return service.search(query, knowledgeBaseId, materialId);
    }
}

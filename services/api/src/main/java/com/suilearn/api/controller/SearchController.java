package com.suilearn.api.controller;

import com.suilearn.api.model.SearchResult;
import com.suilearn.api.search.application.SearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    List<SearchResult> search(
        @RequestParam("q") String query,
        @RequestParam(required = false) String knowledgeBaseId,
        @RequestParam(required = false) String materialId
    ) {
        return searchService.search(query, knowledgeBaseId, materialId);
    }
}

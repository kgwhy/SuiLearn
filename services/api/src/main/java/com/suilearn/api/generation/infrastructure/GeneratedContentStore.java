package com.suilearn.api.generation.infrastructure;

import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GeneratedContentStore {
    private final SuiLearnV2Store store;

    public GeneratedContentStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<GeneratedQuestionDraft> list() {
        return store.listGeneratedContents();
    }

    public Optional<GeneratedQuestionDraft> find(String generatedContentId) {
        return store.findGeneratedContent(generatedContentId);
    }

    public GeneratedQuestionDraft save(GeneratedQuestionDraft draft) {
        return store.saveGeneratedContent(draft);
    }
}

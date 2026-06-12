package com.suilearn.api.knowledgebase.infrastructure;

import com.suilearn.api.model.KnowledgeBase;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeBaseStore {
    private final SuiLearnV2Store store;

    public KnowledgeBaseStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<KnowledgeBase> list() {
        return store.listKnowledgeBases();
    }

    public Optional<KnowledgeBase> find(String id) {
        return store.findKnowledgeBase(id);
    }

    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        return store.saveKnowledgeBase(knowledgeBase);
    }

    public void delete(String knowledgeBaseId) {
        store.deleteKnowledgeBase(knowledgeBaseId);
    }
}

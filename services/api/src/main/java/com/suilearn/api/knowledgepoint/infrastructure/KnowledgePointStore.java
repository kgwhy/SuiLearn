package com.suilearn.api.knowledgepoint.infrastructure;

import com.suilearn.api.model.KnowledgePoint;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgePointStore {
    private final SuiLearnV2Store store;

    public KnowledgePointStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public List<KnowledgePoint> list(String knowledgeBaseId) {
        return store.listKnowledgePoints(knowledgeBaseId);
    }

    public List<KnowledgePoint> list() {
        return store.listKnowledgePoints();
    }

    public Optional<KnowledgePoint> find(String knowledgePointId) {
        return store.findKnowledgePoint(knowledgePointId);
    }

    public KnowledgePoint save(KnowledgePoint point) {
        return store.saveKnowledgePoint(point);
    }

    public void delete(String knowledgePointId) {
        store.deleteKnowledgePoint(knowledgePointId);
    }

    public void markKnowledgePointsSourceOutdated(String materialId, String currentRevisionId) { store.markKnowledgePointsSourceOutdated(materialId, currentRevisionId); }
}

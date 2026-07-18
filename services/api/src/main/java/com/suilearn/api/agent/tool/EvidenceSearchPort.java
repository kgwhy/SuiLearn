package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import java.util.List;

@FunctionalInterface
public interface EvidenceSearchPort {
    List<EvidencePointer> search(SearchRequest request);

    record SearchRequest(String query, AgentScope scope, int limit) {
        public SearchRequest {
            query = RequiredText.value(query, "query");
            if (scope == null) {
                throw new IllegalArgumentException("scope is required");
            }
            if (limit < 1) {
                throw new IllegalArgumentException("limit must be positive");
            }
        }
    }
}

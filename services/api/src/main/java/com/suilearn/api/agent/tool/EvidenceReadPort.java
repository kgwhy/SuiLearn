package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.runtime.StudyScope;
import java.util.Optional;

@FunctionalInterface
public interface EvidenceReadPort {
    Optional<EvidenceRecord> read(ReadRequest request);

    record ReadRequest(String query, EvidencePointer pointer, StudyScope scope) {
        public ReadRequest {
            query = RequiredText.value(query, "query");
            if (pointer == null || scope == null) {
                throw new IllegalArgumentException("pointer and scope are required");
            }
        }
    }
}

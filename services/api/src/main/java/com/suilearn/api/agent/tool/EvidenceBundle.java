package com.suilearn.api.agent.tool;

import java.util.List;

public record EvidenceBundle(List<Item> items) {
    public EvidenceBundle {
        items = List.copyOf(items == null ? List.of() : items);
    }

    public record Item(
        String stableId,
        String sourceRef,
        String content,
        double relevance,
        boolean verified,
        boolean untrusted,
        String materialId,
        String revisionId,
        Integer pageNumber,
        String blockId,
        String excerpt
    ) {
        public Item {
            stableId = RequiredText.value(stableId, "stableId");
            sourceRef = RequiredText.value(sourceRef, "sourceRef");
            content = RequiredText.value(content, "content");
            if (!Double.isFinite(relevance) || relevance < 0.0d || relevance > 1.0d) {
                throw new IllegalArgumentException("relevance must be between 0 and 1");
            }
        }

        public Item(String stableId, String sourceRef, String content, double relevance,
                    boolean verified, boolean untrusted) {
            this(stableId, sourceRef, content, relevance, verified, untrusted,
                null, null, null, null, null);
        }
    }
}

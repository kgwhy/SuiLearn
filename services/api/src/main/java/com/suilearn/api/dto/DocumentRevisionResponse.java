package com.suilearn.api.dto;

import java.time.Instant;
import java.util.List;

public record DocumentRevisionResponse(String id, String materialId, String origin, String processingVersion,
                                       int blockCount, int pageCount, Instant createdAt, List<DocumentBlockResponse> blocks) {
}

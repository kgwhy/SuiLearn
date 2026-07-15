package com.suilearn.api.dto;

import java.util.List;

public record MaterialReadingResponse(String materialId, String revisionId, String origin, String mediaType,
                                     String content, List<DocumentBlockResponse> blocks) {
}

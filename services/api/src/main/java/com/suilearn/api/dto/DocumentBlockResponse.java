package com.suilearn.api.dto;

import java.util.List;

public record DocumentBlockResponse(String id, String revisionId, int ordinal, List<String> sectionPath,
                                    Integer pageNumber, String content) {
}

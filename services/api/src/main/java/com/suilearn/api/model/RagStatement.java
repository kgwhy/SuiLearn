package com.suilearn.api.model;

import java.util.List;

public record RagStatement(
    String text,
    List<Integer> citations
) {
}

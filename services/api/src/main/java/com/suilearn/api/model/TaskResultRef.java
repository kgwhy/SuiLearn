package com.suilearn.api.model;

public record TaskResultRef(
    String type,
    String id,
    Integer count
) {
}

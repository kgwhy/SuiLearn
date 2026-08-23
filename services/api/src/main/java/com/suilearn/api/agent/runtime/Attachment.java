package com.suilearn.api.agent.runtime;

public record Attachment(String attachmentId, String mediaType, String reference) {
    public Attachment {
        attachmentId = requireText(attachmentId, "attachmentId");
        mediaType = mediaType == null || mediaType.isBlank() ? "application/octet-stream" : mediaType.strip();
        reference = reference == null || reference.isBlank() ? null : reference.strip();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}

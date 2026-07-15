package com.suilearn.api.dto;

import com.suilearn.api.model.TaskLifecycleStatus;

/** Response returned when an import has been durably admitted for asynchronous processing. */
public record MaterialImportAcceptedResponse(
    String taskId,
    TaskLifecycleStatus status,
    String taskHref,
    String materialId,
    String materialHref
) {
}

package com.suilearn.api.dto;

import com.suilearn.api.model.TaskLifecycleStatus;

/** User-safe reference to a durably admitted asynchronous task. */
public record TaskSubmissionResponse(String taskId, TaskLifecycleStatus status, String taskHref) { }

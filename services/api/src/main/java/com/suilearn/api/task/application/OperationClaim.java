package com.suilearn.api.task.application;

/** Result of a durable adapter-operation claim. */
public record OperationClaim(String operationId, OperationClaimDisposition disposition, String resultReference) { }

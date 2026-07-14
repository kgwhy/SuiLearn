package com.suilearn.api.task.application;

record OperationClaim(String operationId, OperationClaimDisposition disposition, String resultReference) { }

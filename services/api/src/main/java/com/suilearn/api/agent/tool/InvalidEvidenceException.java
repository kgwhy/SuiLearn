package com.suilearn.api.agent.tool;

public final class InvalidEvidenceException extends IllegalArgumentException {
    public InvalidEvidenceException() {
        super("INVALID_EVIDENCE_REFERENCE");
    }
}

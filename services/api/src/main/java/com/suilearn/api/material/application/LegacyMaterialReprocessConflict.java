package com.suilearn.api.material.application;

/** The immutable legacy text revision is readable but has no original binary to process again. */
public final class LegacyMaterialReprocessConflict extends RuntimeException {
    public LegacyMaterialReprocessConflict() { super("LEGACY_NO_ORIGINAL"); }
}

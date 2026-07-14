package com.suilearn.api.material.storage;

record StagedAsset(String temporaryKey, String checksum, long sizeBytes, String mimeType) { }

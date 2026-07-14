package com.suilearn.api.material.storage;

import java.time.Instant;

public record StoredObject(String key, Instant lastModifiedAt) { }

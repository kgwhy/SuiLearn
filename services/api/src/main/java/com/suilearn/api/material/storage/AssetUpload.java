package com.suilearn.api.material.storage;

import java.io.InputStream;

public record AssetUpload(InputStream stream, String originalFilename, String mimeType) { }

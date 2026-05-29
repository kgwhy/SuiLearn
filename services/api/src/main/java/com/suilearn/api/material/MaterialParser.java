package com.suilearn.api.material;

import com.suilearn.api.model.MaterialSourceType;

public interface MaterialParser {
    ParsedMaterial parse(ParseRequest request);

    record ParseRequest(
        String title,
        String fileName,
        MaterialSourceType sourceType,
        String content
    ) {
    }

    record ParsedMaterial(
        String content
    ) {
    }
}

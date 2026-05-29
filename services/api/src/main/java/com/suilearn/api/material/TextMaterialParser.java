package com.suilearn.api.material;

import org.springframework.stereotype.Component;

@Component
public class TextMaterialParser implements MaterialParser {
    @Override
    public ParsedMaterial parse(ParseRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Material content is required");
        }
        var normalizedContent = request.content()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim();
        return new ParsedMaterial(normalizedContent);
    }
}

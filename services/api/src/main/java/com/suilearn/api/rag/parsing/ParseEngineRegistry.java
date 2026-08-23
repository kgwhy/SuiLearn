package com.suilearn.api.rag.parsing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParseEngineRegistry {
    private final Map<String, ParseEngine> engines;

    public ParseEngineRegistry(List<ParseEngine> engines) {
        var copy = new LinkedHashMap<String, ParseEngine>();
        for (ParseEngine engine : engines) copy.put(engine.name(), engine);
        this.engines = Map.copyOf(copy);
    }

    public ParsedDocument parse(String mediaType, byte[] content) {
        return engines.values().stream().filter(engine -> engine.supports(mediaType)).findFirst()
            .map(engine -> engine.parse(mediaType, content))
            .orElseThrow(() -> new IllegalArgumentException("unsupported parse media type: " + mediaType));
    }

    public List<ParseEngine> engines() { return List.copyOf(engines.values()); }
}

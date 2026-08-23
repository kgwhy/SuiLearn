package com.suilearn.api.rag.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PipelineFactory {
    private final Map<String, RagPipeline> pipelines;

    public PipelineFactory(Map<String, RagPipeline> pipelines) {
        var copy = new LinkedHashMap<String, RagPipeline>();
        pipelines.forEach((key, pipeline) -> copy.put(pipeline.name(), pipeline));
        this.pipelines = Map.copyOf(copy);
    }

    public static PipelineFactory defaults(com.suilearn.api.retrieval.Retriever retriever) {
        var pipeline = new PgvectorHybridRagPipeline(retriever);
        return new PipelineFactory(Map.of(pipeline.name(), pipeline));
    }

    public RagPipeline pipeline(String name) {
        String effective = name == null || name.isBlank() ? PgvectorHybridRagPipeline.NAME : name;
        RagPipeline pipeline = pipelines.get(effective);
        if (pipeline == null) {
            throw new IllegalArgumentException("unknown rag pipeline: " + effective);
        }
        return pipeline;
    }
}

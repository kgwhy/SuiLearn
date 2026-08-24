package com.suilearn.api.rag.config;

import com.suilearn.api.material.document.DocumentParser;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.rag.index.EmbeddingIndexVersionRecorder;
import com.suilearn.api.rag.index.IndexVersionManager;
import com.suilearn.api.rag.index.IndexVersionRepository;
import com.suilearn.api.rag.parsing.DocumentParseEngine;
import com.suilearn.api.rag.parsing.OcrParseEngine;
import com.suilearn.api.rag.parsing.ParseEngineRegistry;
import com.suilearn.api.rag.parsing.TextParseEngine;
import com.suilearn.api.rag.pipeline.PgvectorHybridRagPipeline;
import com.suilearn.api.rag.pipeline.PipelineFactory;
import com.suilearn.api.rag.pipeline.RagPipeline;
import com.suilearn.api.retrieval.Retriever;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RagInfrastructureConfiguration {
    @Bean
    PgvectorHybridRagPipeline pgvectorHybridRagPipeline(Retriever retriever) {
        return new PgvectorHybridRagPipeline(retriever);
    }

    @Bean
    PipelineFactory ragPipelineFactory(Map<String, RagPipeline> pipelines) {
        return new PipelineFactory(pipelines);
    }

    @Bean
    ParseEngineRegistry ragParseEngineRegistry(TesseractOcrAdapter ocr) {
        var parser = new DocumentParser();
        return new ParseEngineRegistry(List.of(
            new TextParseEngine(),
            new DocumentParseEngine("pdf", "application/pdf", "document.pdf", parser),
            new DocumentParseEngine("doc", "application/msword", "document.doc", parser),
            new DocumentParseEngine("docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "document.docx", parser),
            new OcrParseEngine(ocr)
        ));
    }

    @Bean
    IndexVersionManager ragIndexVersionManager(IndexVersionRepository repository, Clock clock) {
        return new IndexVersionManager(repository, clock);
    }

    @Bean
    EmbeddingIndexVersionRecorder embeddingIndexVersionRecorder(IndexVersionManager manager) {
        return new EmbeddingIndexVersionRecorder(manager);
    }
}

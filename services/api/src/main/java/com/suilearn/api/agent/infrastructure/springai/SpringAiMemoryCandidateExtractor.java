package com.suilearn.api.agent.infrastructure.springai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.application.MemoryCandidateExtractor;
import com.suilearn.api.agent.memory.MemoryCandidate;
import com.suilearn.api.agent.memory.MemoryFingerprint;
import com.suilearn.api.agent.memory.MemoryType;
import com.suilearn.api.agent.prompt.PromptRegistry;
import com.suilearn.api.agent.prompt.PromptVariables;
import com.suilearn.api.agent.prompt.StructuredOutputProcessor;
import com.suilearn.api.agent.prompt.ValidationResult;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

final class SpringAiMemoryCandidateExtractor implements MemoryCandidateExtractor {
    private static final String SCHEMA =
        "candidate is null or has type, content, confidence, and sourceRef fields";
    private final ChatModel model;
    private final PromptRegistry prompts;
    private final ObjectMapper objectMapper;

    SpringAiMemoryCandidateExtractor(ChatModel model, PromptRegistry prompts, ObjectMapper objectMapper) {
        this.model = model;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<MemoryCandidate> extract(Request request) {
        String rendered = prompts.render("memory-extraction", "v1", new PromptVariables.MemoryExtraction(
            request.verifiedOutcome(), String.join(",", request.sourceReferences()), SCHEMA)).content();
        StructuredOutputProcessor<Envelope> processor = new StructuredOutputProcessor<>(this::decode,
            envelope -> validate(envelope, request.sourceReferences()),
            (invalid, reasons) -> call(rendered + "\nValidation codes: " + String.join(",", reasons)));
        Envelope envelope = processor.process(call(rendered)).value();
        if (envelope.candidate() == null) {
            return Optional.empty();
        }
        Candidate candidate = envelope.candidate();
        return Optional.of(new MemoryCandidate(request.learnerId(), candidate.type(), candidate.content(),
            MemoryFingerprint.of(candidate.content()), candidate.confidence(), request.runId(), candidate.sourceRef()));
    }

    private String call(String prompt) {
        var response = model.call(new Prompt(prompt));
        return response.getResult().getOutput().getText();
    }

    private Envelope decode(String raw) {
        try {
            return objectMapper.readValue(raw, Envelope.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("INVALID_MODEL_OUTPUT");
        }
    }

    private ValidationResult validate(Envelope envelope, List<String> allowedRefs) {
        if (envelope == null || envelope.candidate() == null) {
            return ValidationResult.success();
        }
        Candidate candidate = envelope.candidate();
        var reasons = new java.util.ArrayList<String>();
        if (candidate.type() == null) reasons.add("TYPE_REQUIRED");
        if (candidate.content() == null || candidate.content().isBlank()) reasons.add("CONTENT_REQUIRED");
        if (!Double.isFinite(candidate.confidence()) || candidate.confidence() < 0 || candidate.confidence() > 1)
            reasons.add("CONFIDENCE_INVALID");
        if (candidate.sourceRef() == null || !allowedRefs.contains(candidate.sourceRef()))
            reasons.add("SOURCE_OUT_OF_SCOPE");
        return reasons.isEmpty() ? ValidationResult.success() : new ValidationResult(false, reasons);
    }

    record Envelope(Candidate candidate) { }
    record Candidate(MemoryType type, String content, double confidence, String sourceRef) { }
}

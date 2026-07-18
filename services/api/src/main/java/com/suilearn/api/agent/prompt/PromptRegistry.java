package com.suilearn.api.agent.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

public final class PromptRegistry {
    private static final Map<PromptKey, Registration> CATALOG = Map.of(
        new PromptKey("supervisor", "v1"),
        new Registration("classpath:agents/supervisor/v1/system.md", PromptVariables.Supervisor.class),
        new PromptKey("knowledge-research", "v1"),
        new Registration("classpath:agents/knowledge-research/v1/system.md", PromptVariables.KnowledgeResearch.class),
        new PromptKey("practice-coach", "v1"),
        new Registration("classpath:agents/practice-coach/v1/system.md", PromptVariables.PracticeCoach.class),
        new PromptKey("memory-extraction", "v1"),
        new Registration("classpath:agents/memory-extraction/v1/system.md", PromptVariables.MemoryExtraction.class)
    );

    private final ResourceLoader resourceLoader;
    private final PromptTemplateRenderer templateRenderer;

    public PromptRegistry(PromptTemplateRenderer templateRenderer) {
        this(new DefaultResourceLoader(), templateRenderer);
    }

    public PromptRegistry(ResourceLoader resourceLoader, PromptTemplateRenderer templateRenderer) {
        this.resourceLoader = resourceLoader;
        this.templateRenderer = java.util.Objects.requireNonNull(templateRenderer, "templateRenderer");
    }

    public Set<PromptKey> allowlist() {
        return CATALOG.keySet();
    }

    public PromptDocument load(String name, String version) {
        PromptKey key = new PromptKey(name, version);
        Registration registration = registration(key);
        String content = read(registration.location());
        return new PromptDocument(key.name(), key.version(), sha256(content), content);
    }

    public RenderedPrompt render(String name, String version, PromptVariables variables) {
        PromptKey key = new PromptKey(name, version);
        Registration registration = registration(key);
        if (!registration.variableType().isInstance(variables)) {
            throw new IllegalArgumentException("typed variables do not match registered prompt");
        }
        PromptDocument document = load(name, version);
        String rendered;
        try {
            rendered = templateRenderer.render(document.content(), variables.values());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("typed variables do not match prompt template");
        }
        return new RenderedPrompt(document.name(), document.version(), document.sha256(), rendered);
    }

    private Registration registration(PromptKey key) {
        Registration registration = CATALOG.get(key);
        if (registration == null) {
            throw new UnknownPromptException(key.name(), key.version());
        }
        return registration;
    }

    private String read(String location) {
        try (InputStream input = resourceLoader.getResource(location).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PromptResourceException("registered prompt resource is unavailable", exception);
        }
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private record Registration(String location, Class<? extends PromptVariables> variableType) {
    }

    public record RenderedPrompt(String name, String version, String sha256, String content) {
    }

    public static final class UnknownPromptException extends IllegalArgumentException {
        public UnknownPromptException(String name, String version) {
            super("prompt is not registered: " + name + "/" + version);
        }
    }

    public static final class PromptResourceException extends IllegalStateException {
        public PromptResourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

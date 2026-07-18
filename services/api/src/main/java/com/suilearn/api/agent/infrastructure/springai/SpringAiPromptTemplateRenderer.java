package com.suilearn.api.agent.infrastructure.springai;

import com.suilearn.api.agent.prompt.PromptTemplateRenderer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.st.StTemplateRenderer;

public final class SpringAiPromptTemplateRenderer implements PromptTemplateRenderer {
    private final TemplateRenderer delegate = StTemplateRenderer.builder()
        .startDelimiterToken('<')
        .endDelimiterToken('>')
        .build();

    @Override
    public String render(String template, Map<String, String> variables) {
        return delegate.apply(template, new LinkedHashMap<>(variables));
    }
}

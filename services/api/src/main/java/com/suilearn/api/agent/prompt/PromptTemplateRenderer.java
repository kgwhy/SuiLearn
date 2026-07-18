package com.suilearn.api.agent.prompt;

import java.util.Map;

@FunctionalInterface
public interface PromptTemplateRenderer {
    String render(String template, Map<String, String> variables);
}

package com.suilearn.api.agent.infrastructure.springai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.prompt.PromptRegistry;
import com.suilearn.api.agent.prompt.PromptVariables;
import com.suilearn.api.agent.tool.PracticeModelPort;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

final class SpringAiPracticeModelPort implements PracticeModelPort {
    private static final String OUTPUT_SCHEMA =
        "{explanation:string,exercises:[{question,answer,explanation,citations[]}],citations:[],nextStep:string,requestedAction:NONE}";
    private final ChatModel model;
    private final PromptRegistry prompts;
    private final ObjectMapper objectMapper;

    SpringAiPracticeModelPort(ChatModel model, PromptRegistry prompts, ObjectMapper objectMapper) {
        this.model = model;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
    }

    @Override
    public Draft generate(Request request) {
        String prompt = prompts.render("practice-coach", "v1", new PromptVariables.PracticeCoach(
            request.learningGoal(), request.evidence().toString(), request.difficulty().name(),
            Integer.toString(request.practiceCount()), OUTPUT_SCHEMA)).content();
        String raw = model.call(new Prompt(prompt)).getResult().getOutput().getText();
        try {
            return objectMapper.readValue(raw, Draft.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("INVALID_MODEL_OUTPUT");
        }
    }
}

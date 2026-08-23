package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import com.suilearn.api.agent.runtime.TurnContext;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GeneratePracticeTool implements Tool {
    private final PracticeCoachSubAgent coach;

    public GeneratePracticeTool(PracticeCoachSubAgent coach) {
        this.coach = coach;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(AgentToolNames.GENERATE_PRACTICE,
            "Generate temporary practice items from verified evidence. Results are never saved to formal stores.",
            Map.of("type", "object", "properties", Map.of(
                "learningGoal", Map.of("type", "string", "minLength", 1),
                "difficulty", Map.of("type", "string", "enum", List.of("EASY", "MEDIUM", "HARD"), "default", "MEDIUM"),
                "practiceCount", Map.of("type", "integer", "minimum", 1, "maximum", 5, "default", 3),
                "evidence", Map.of("type", "array", "minItems", 1, "items", Map.of(
                    "type", "object",
                    "required", List.of("stableId", "sourceRef", "content", "relevance"),
                    "properties", Map.of(
                        "stableId", Map.of("type", "string"),
                        "sourceRef", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "relevance", Map.of("type", "number", "minimum", 0, "maximum", 1)
                    )))
            ), "required", List.of("learningGoal", "evidence")),
            false, Set.of("turn", "temporary"));
    }

    @Override
    public ToolResult execute(TurnContext context, Map<String, Object> args) {
        String learningGoal = ToolArguments.requiredString(args, "learningGoal", 4000);
        String difficultyText = ToolArguments.optionalString(args, "difficulty", 16);
        Difficulty difficulty = difficultyText == null ? Difficulty.MEDIUM : Difficulty.valueOf(difficultyText);
        int practiceCount = ToolArguments.integer(args, "practiceCount", 3, 1, 5);
        List<EvidenceBundle.Item> evidence = evidence(args);
        if (coach == null) {
            return new ToolResult("Practice generation is unavailable.", List.of(),
                Map.of("code", "AGENT_MODEL_UNAVAILABLE"), false, null);
        }
        try {
            var result = coach.coach(new PracticeCoachSubAgent.Request(learningGoal,
                    new EvidenceBundle(evidence), difficulty, practiceCount),
                new SharedAgentBudget(1, 1, 2, Duration.ofSeconds(30), Clock.systemUTC()));
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("uncertain", result.uncertain());
            metadata.put("explanation", result.explanation());
            metadata.put("nextStep", result.nextStep());
            metadata.put("citations", result.citations());
            metadata.put("exercises", result.exercises().stream().map(GeneratePracticeTool::exercise).toList());
            var sources = result.citations().stream()
                .map(sourceRef -> new ToolCitation(sourceRef, sourceRef)).toList();
            return new ToolResult(result.explanation(), sources, metadata, true, null);
        } catch (RuntimeException exception) {
            return new ToolResult("Practice generation failed.", List.of(),
                Map.of("code", "PRACTICE_GENERATION_FAILED"), false, null);
        }
    }

    private static List<EvidenceBundle.Item> evidence(Map<String, Object> args) {
        return ToolArguments.mapList(args, "evidence").stream().map(item -> {
            String stableId = ToolArguments.requiredString(item, "stableId", 256);
            String sourceRef = ToolArguments.requiredString(item, "sourceRef", 256);
            String content = ToolArguments.requiredString(item, "content", 16_000);
            double relevance = ToolArguments.decimal(item, "relevance", 0.0d, 0.0d, 1.0d);
            return new EvidenceBundle.Item(stableId, sourceRef, content, relevance, true, true);
        }).toList();
    }

    private static Map<String, Object> exercise(TemporaryExercise exercise) {
        var item = new LinkedHashMap<String, Object>();
        item.put("question", exercise.question());
        item.put("answer", exercise.answer());
        item.put("explanation", exercise.explanation());
        item.put("citations", exercise.citations());
        return item;
    }
}

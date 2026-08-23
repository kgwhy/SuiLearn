package com.suilearn.api.agent.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LegacyRetirementScanTest {
    @Test
    void legacyAgentRuntimeAndRestAreAbsent() throws Exception {
        var violations = new ArrayList<String>();
        for (Path root : List.of(Path.of("src", "main", "java"), Path.of("src", "test", "java"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    if (file.toString().contains("LegacyRetirementScanTest.java")) {
                        continue;
                    }
                    String text = Files.readString(file);
                    if (text.contains("LearningAgentPort") || text.contains("ReactAgent")
                        || text.contains("com.alibaba.cloud.ai") || text.contains("SpringAiAlibabaLearningAgentAdapter")) {
                        violations.add(file.toString());
                    }
                }
            }
        }
        assertThat(violations).isEmpty();

        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).doesNotContain("spring-ai-alibaba-agent-framework");

        String contract = Files.readString(Path.of("..", "..", "contracts", "openapi", "suilearn-v2.yaml"));
        assertThat(contract).doesNotContain("/api/v2/agents/study", "StudyAgentRunRequest", "StudyAgentError");
    }
}

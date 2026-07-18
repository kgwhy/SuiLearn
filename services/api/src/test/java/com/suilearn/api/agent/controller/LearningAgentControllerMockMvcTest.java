package com.suilearn.api.agent.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.metrics.AgentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LearningAgentControllerMockMvcTest {
    @Test
    void exposesDistinctRequiredMismatchAndNotFoundScopeErrors() throws Exception {
        LearningAgentPort port = request -> {
            if (request.scope().knowledgeBaseId() != null && request.scope().materialId() != null) {
                throw new IllegalArgumentException("AGENT_SCOPE_MISMATCH: internal-scope-body");
            }
            if ("missing".equals(request.scope().knowledgeBaseId())) {
                throw new IllegalArgumentException("AGENT_SCOPE_NOT_FOUND: internal-scope-body");
            }
            return LearningAgentPort.StudyRunResult.noEvidence("run", "generated-session",
                new LearningAgentPort.BudgetUsage(0, 1, 0, 1, 0, 1, 0, 90_000, false));
        };
        var controller = new LearningAgentController(port, null, true,
            LearningAgentControllerTest.properties(), new AgentMetrics(new SimpleMeterRegistry()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new LearningAgentExceptionHandler()).build();

        mvc.perform(post("/api/v2/agents/study/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"learnerId\":\"learner\",\"question\":\"q\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("AGENT_SCOPE_REQUIRED"));

        mvc.perform(post("/api/v2/agents/study/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"learnerId\":\"learner\",\"question\":\"q\",\"knowledgeBaseId\":\"kb\",\"materialId\":\"m\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("AGENT_SCOPE_MISMATCH"))
            .andExpect(jsonPath("$.message").value("The requested scopes are inconsistent."));

        mvc.perform(post("/api/v2/agents/study/runs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"learnerId\":\"learner\",\"question\":\"q\",\"knowledgeBaseId\":\"missing\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("AGENT_SCOPE_NOT_FOUND"));
    }
}

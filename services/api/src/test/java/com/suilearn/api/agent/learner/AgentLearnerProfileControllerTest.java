package com.suilearn.api.agent.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.AgentLearnerProfileController;
import com.suilearn.api.agent.controller.LearnerProfileDtos.LearnerProfileRequest;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerAuthenticationToken;
import com.suilearn.api.security.LearnerPrincipal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentLearnerProfileControllerTest {
    @Test
    void authenticatedLearnerCanReadAndUpsertOwnProfile() {
        var profiles = mock(LearnerProfileService.class);
        when(profiles.get("learner-a")).thenReturn(Optional.of(
            new LearnerProfile("learner-a", "visual learner", List.of("Java"))));
        when(profiles.save("learner-a", "updated", List.of("Spring")))
            .thenReturn(new LearnerProfile("learner-a", "updated", List.of("Spring")));
        var controller = new AgentLearnerProfileController(profiles, properties(true), auth(true));
        var authentication = new LearnerAuthenticationToken(new LearnerPrincipal("learner-a"));

        var read = controller.get("learner-a", authentication);
        assertThat(read.persona()).isEqualTo("visual learner");

        var written = controller.put("learner-a", new LearnerProfileRequest("updated", List.of("Spring")), authentication);
        assertThat(written.skills()).containsExactly("Spring");
        verify(profiles).save("learner-a", "updated", List.of("Spring"));
    }

    @Test
    void crossLearnerProfileIsNotFound() {
        var profiles = mock(LearnerProfileService.class);
        var controller = new AgentLearnerProfileController(profiles, properties(true), auth(true));
        var authentication = new LearnerAuthenticationToken(new LearnerPrincipal("learner-a"));

        assertThatThrownBy(() -> controller.get("learner-b", authentication))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_FOUND));
        assertThatThrownBy(() -> controller.put("learner-b",
            new LearnerProfileRequest("", List.of()), authentication))
            .isInstanceOfSatisfying(TurnApiException.class, error ->
                assertThat(error.code()).isEqualTo(TurnErrorCode.AGENT_TURN_NOT_FOUND));
    }

    private AgentConfigurationProperties properties(boolean enabled) {
        return new AgentConfigurationProperties(enabled, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }

    private AgentAuthProperties auth(boolean enabled) {
        var auth = new AgentAuthProperties();
        auth.setEnabled(enabled);
        return auth;
    }
}

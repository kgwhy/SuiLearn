package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.LearnerProfileDtos.LearnerProfileRequest;
import com.suilearn.api.agent.controller.LearnerProfileDtos.LearnerProfileResponse;
import com.suilearn.api.agent.learner.LearnerProfileService;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentLearnerProfileController {
    private final LearnerProfileService profiles;
    private final AgentConfigurationProperties agentProperties;
    private final AgentAuthProperties authProperties;

    public AgentLearnerProfileController(LearnerProfileService profiles,
                                         AgentConfigurationProperties agentProperties,
                                         AgentAuthProperties authProperties) {
        this.profiles = profiles;
        this.agentProperties = agentProperties;
        this.authProperties = authProperties;
    }

    @GetMapping("/api/v2/agent/learners/{learnerId}/profile")
    public LearnerProfileResponse get(@PathVariable @Size(max = 128) String learnerId,
                                      Authentication authentication) {
        requireEnabled(learnerId, authentication);
        return profiles.get(learnerId)
            .map(LearnerProfileResponse::from)
            .orElseThrow(() -> new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND));
    }

    @PutMapping("/api/v2/agent/learners/{learnerId}/profile")
    public LearnerProfileResponse put(@PathVariable @Size(max = 128) String learnerId,
                                      @Valid @RequestBody LearnerProfileRequest request,
                                      Authentication authentication) {
        requireEnabled(learnerId, authentication);
        var profile = profiles.save(learnerId, request.persona(), request.skills());
        return LearnerProfileResponse.from(profile);
    }

    private void requireEnabled(String pathLearnerId, Authentication authentication) {
        if (!agentProperties.enabled()) {
            throw new TurnApiException(TurnErrorCode.AGENT_FEATURE_DISABLED);
        }
        if (authProperties.isEnabled()) {
            LearnerPrincipal principal = LearnerPrincipal.fromAuthentication(authentication);
            if (principal == null || !principal.learnerId().equals(pathLearnerId)) {
                throw new TurnApiException(TurnErrorCode.AGENT_TURN_NOT_FOUND);
            }
        }
    }
}

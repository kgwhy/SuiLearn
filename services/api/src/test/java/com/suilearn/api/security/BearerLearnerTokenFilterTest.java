package com.suilearn.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class BearerLearnerTokenFilterTest {
    @Test
    void validTokenPopulatesAndClearsSecurityContext() throws Exception {
        var filter = new BearerLearnerTokenFilter(new LearnerTokenRegistry(List.of(
            new LearnerTokenRegistry.TokenBinding("valid-token", "learner-a"))));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertThat(LearnerPrincipal.fromAuthentication(
            SecurityContextHolder.getContext().getAuthentication()).learnerId()).isEqualTo("learner-a");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenIsForbiddenAndMissingTokenContinues() throws Exception {
        var filter = new BearerLearnerTokenFilter(new LearnerTokenRegistry(List.of(
            new LearnerTokenRegistry.TokenBinding("valid-token", "learner-a"))));
        var invalid = new MockHttpServletRequest();
        invalid.addHeader("Authorization", "Bearer bad-token-value");
        var invalidResponse = new MockHttpServletResponse();
        filter.doFilter(invalid, invalidResponse, (req, res) -> { });
        assertThat(invalidResponse.getStatus()).isEqualTo(403);

        var missing = new MockHttpServletRequest();
        var missingResponse = new MockHttpServletResponse();
        filter.doFilter(missing, missingResponse, (req, res) -> { });
        assertThat(missingResponse.getStatus()).isEqualTo(200);
    }
}

package com.suilearn.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class BearerLearnerTokenFilter extends OncePerRequestFilter {
    private final LearnerTokenRegistry registry;

    public BearerLearnerTokenFilter(LearnerTokenRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid bearer token");
            return;
        }
        String token = header.substring(7).strip();
        Optional<LearnerPrincipal> principal = registry.resolve(token);
        if (principal.isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid bearer token");
            return;
        }
        var authentication = new LearnerAuthenticationToken(principal.orElseThrow());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

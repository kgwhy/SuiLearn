package com.suilearn.api.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class LearnerAuthenticationToken extends AbstractAuthenticationToken {
    private final LearnerPrincipal principal;

    public LearnerAuthenticationToken(LearnerPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_LEARNER")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override public Object getCredentials() { return null; }
    @Override public Object getPrincipal() { return principal; }
    public String learnerId() { return principal.learnerId(); }
}

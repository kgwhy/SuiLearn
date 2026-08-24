package com.suilearn.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("suilearn.auth")
public class AgentAuthProperties {
    private boolean enabled = false;
    private String tokens = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTokens() { return tokens == null ? "" : tokens; }
    public void setTokens(String tokens) { this.tokens = tokens; }
}

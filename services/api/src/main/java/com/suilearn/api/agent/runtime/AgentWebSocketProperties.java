package com.suilearn.api.agent.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("suilearn.agent.websocket")
public class AgentWebSocketProperties {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

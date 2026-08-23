package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.CapabilityRegistry;
import com.suilearn.api.agent.runtime.ToolRegistry;
import com.suilearn.api.agent.runtime.TurnApiException;
import com.suilearn.api.agent.runtime.TurnErrorCode;
import com.suilearn.api.agent.tool.ToolDefinition;
import com.suilearn.api.agent.controller.CapabilityDtos.CapabilitiesResponse;
import com.suilearn.api.agent.controller.CapabilityDtos.CapabilityDescriptor;
import com.suilearn.api.agent.controller.CapabilityDtos.ToolDescriptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentCapabilitiesController {
    private final CapabilityRegistry capabilities;
    private final ToolRegistry tools;
    private final AgentConfigurationProperties properties;

    public AgentCapabilitiesController(CapabilityRegistry capabilities, ToolRegistry tools,
                                       AgentConfigurationProperties properties) {
        this.capabilities = capabilities;
        this.tools = tools;
        this.properties = properties;
    }

    @GetMapping("/api/v2/agent/capabilities")
    public CapabilitiesResponse capabilities() {
        requireEnabled();
        var capabilityDescriptors = capabilities.manifests().stream()
            .map(AgentCapabilitiesController::capability).toList();
        var toolDescriptors = tools.definitions().stream()
            .map(AgentCapabilitiesController::tool).toList();
        return new CapabilitiesResponse(capabilityDescriptors, toolDescriptors);
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new TurnApiException(TurnErrorCode.AGENT_FEATURE_DISABLED);
        }
    }

    private static CapabilityDescriptor capability(CapabilityManifest manifest) {
        return new CapabilityDescriptor(manifest.name(), manifest.description(), manifest.ownedTools());
    }

    private static ToolDescriptor tool(ToolDefinition definition) {
        return new ToolDescriptor(definition.name(), definition.description(), definition.parameters(),
            definition.deferred(), definition.requiredScopes());
    }
}

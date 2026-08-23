package com.suilearn.api.agent.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AgentCapabilitiesOpenApiContractTest {
    @Test
    @SuppressWarnings("unchecked")
    void capabilitiesPathAndSchemasAreAdditive() throws Exception {
        String contract = Files.readString(Path.of("..", "..", "contracts", "openapi", "suilearn-v2.yaml"));
        Map<String, Object> document = new Yaml().load(contract);
        Map<String, Object> paths = map(document.get("paths"));
        assertThat(paths).containsKey("/api/v2/agent/capabilities");
        assertThat(paths).containsKey("/api/v2/agent/turns");

        Map<String, Object> schemas = map(map(document.get("components")).get("schemas"));
        Map<String, Object> response = map(schemas.get("AgentCapabilitiesResponse"));
        assertThat((List<String>) response.get("required")).containsExactlyInAnyOrder("capabilities", "tools");
        Map<String, Object> tool = map(schemas.get("AgentToolDescriptor"));
        assertThat((List<String>) tool.get("required"))
            .containsExactlyInAnyOrder("name", "description", "parameters", "deferred", "requiredScopes");
        assertThat(contract).contains("AgentCapabilityDescriptor", "AgentCapabilitiesResponse");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}

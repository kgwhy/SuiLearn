package com.suilearn.api.agent.capability;

import com.suilearn.api.agent.tool.AgentToolNames;
import java.util.List;
import java.util.Set;

public final class BuiltinCapabilities {
    public static final String STUDY_AGENT = "study_agent";
    public static final String RAG_QA = "rag_qa";
    public static final String QUESTION_GENERATION = "question_generation";

    private BuiltinCapabilities() {}

    public static Capability studyAgent() {
        return fixed(STUDY_AGENT, "Bounded study assistant with evidence, practice, memory, and ask-user tools.",
            Set.of(AgentToolNames.SEARCH_KNOWLEDGE, AgentToolNames.READ_EVIDENCE,
                AgentToolNames.GENERATE_PRACTICE, AgentToolNames.RECALL_MEMORY,
                AgentToolNames.PERSIST_MEMORY, AgentToolNames.ASK_USER));
    }

    public static Capability ragQa() {
        return fixed(RAG_QA, "Low-latency evidence-grounded question answering.",
            Set.of(AgentToolNames.SEARCH_KNOWLEDGE, AgentToolNames.READ_EVIDENCE));
    }

    public static Capability questionGeneration() {
        return fixed(QUESTION_GENERATION, "Temporary practice/question generation from structured sources.",
            Set.of(AgentToolNames.GENERATE_PRACTICE, AgentToolNames.ASK_USER));
    }

    public static List<Capability> all() {
        return List.of(studyAgent(), ragQa(), questionGeneration());
    }

    private static Capability fixed(String name, String description, Set<String> ownedTools) {
        var manifest = new CapabilityManifest(name, description, ownedTools);
        return () -> manifest;
    }
}

package com.suilearn.api.agent.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.LlmMessage;
import com.suilearn.api.agent.llm.LlmRequest;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemoryConsolidator {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryConsolidator.class);

    private final MemoryConsolidationCommandRepository commands;
    private final MemorySnapshotRepository snapshots;
    private final MemoryL2DocRepository l2;
    private final MemoryL3DocRepository l3;
    private final MemoryMetaRepository meta;
    private final LlmClient client;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Clock clock;

    public MemoryConsolidator(MemoryConsolidationCommandRepository commands,
                              MemorySnapshotRepository snapshots, MemoryL2DocRepository l2,
                              MemoryL3DocRepository l3, MemoryMetaRepository meta,
                              LlmClient client, ObjectMapper objectMapper, String model, Clock clock) {
        this.commands = commands; this.snapshots = snapshots; this.l2 = l2; this.l3 = l3; this.meta = meta;
        this.client = client; this.objectMapper = objectMapper;
        this.model = model == null || model.isBlank() ? "suilearn-default" : model; this.clock = clock;
    }

    public MemoryConsolidationCommandEntity submitUpdate(String learnerId, String surface, String operationKey) {
        String idem = learnerId + ":" + surface + ":" + operationKey;
        var existing = commands.findByIdempotencyKey(idem);
        if (existing.isPresent()) return existing.get();
        try {
            return commands.save(new MemoryConsolidationCommandEntity(newId(), learnerId, surface, operationKey,
                idem, "PENDING", clock.instant(), null));
        } catch (RuntimeException duplicate) {
            return commands.findByIdempotencyKey(idem).orElseThrow(() -> duplicate);
        }
    }

    public int processDue() {
        int processed = 0;
        for (MemoryConsolidationCommandEntity command : commands.findTop10ByStatusOrderByCreatedAtAsc("PENDING")) {
            try {
                updateL2(command.getLearnerId(), command.getSurface());
                command.markProcessed(clock.instant());
                commands.save(command);
                processed++;
            } catch (RuntimeException failure) {
                LOG.warn("Memory consolidation failed for command {}", command.getId(), failure);
            }
        }
        return processed;
    }

    public void mergeL3(String learnerId) {
        var docs = l2.findByLearnerIdOrderByUpdatedAtDesc(learnerId);
        if (docs.isEmpty()) return;
        String markdown = docs.stream().map(doc -> "## " + doc.getSurface() + "\n" + doc.getContentMd())
            .reduce("", (a, b) -> a.isBlank() ? b : a + "\n\n" + b);
        var response = client.chat(new LlmRequest(model, List.of(
            LlmMessage.system("Return JSON object with slots recent/profile/scope/preferences; each is a short markdown string."),
            LlmMessage.user(markdown)), List.of(), 0.1, null));
        try {
            JsonNode root = objectMapper.readTree(stripFence(response.content()));
            for (String slot : List.of("recent", "profile", "scope", "preferences")) {
                String id = learnerId + ":" + slot;
                String content = root.path(slot).asText(root.path(slot).isObject()
                    ? root.path(slot).toString() : "");
                l3.save(new MemoryL3DocEntity(id, learnerId, slot, content, clock.instant()));
            }
        } catch (Exception failure) {
            throw new IllegalStateException("L3 merge response was invalid", failure);
        }
    }

    private void updateL2(String learnerId, String surface) {
        var pending = snapshots.findByLearnerIdAndConsumedFalseOrderByCreatedAtAsc(learnerId).stream()
            .filter(item -> surface.equals(item.getSurface())).toList();
        if (pending.isEmpty()) return;
        var existing = l2.findByLearnerIdAndSurface(learnerId, surface).orElse(null);
        String input = pending.stream().map(item -> item.getEntityKey() + ":\n" + item.getContent())
            .reduce("", (a, b) -> a.isBlank() ? b : a + "\n\n" + b);
        var response = client.chat(new LlmRequest(model, List.of(
            LlmMessage.system("Generate a concise markdown memory doc with footnote source keys."),
            LlmMessage.user((existing == null ? "" : "Existing:\n" + existing.getContentMd() + "\n\n")
                + "New facts:\n" + input)), List.of(), 0.1, null));
        String content = response.content() == null || response.content().isBlank() ? input : response.content().strip();
        String id = existing == null ? learnerId + ":" + surface : existing.getId();
        l2.save(new MemoryL2DocEntity(id, learnerId, surface, content, "snapshot:" + surface, clock.instant()));
        meta.save(new MemoryMetaEntity(learnerId + ":" + surface, learnerId, surface,
            pending.get(pending.size() - 1).getEntityKey(), clock.instant()));
        pending.forEach(MemorySnapshotEntity::markConsumed);
        snapshots.saveAll(pending);
    }

    private String stripFence(String content) {
        return content == null ? "" : content.strip().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
    }

    private String newId() { return "memcmd_" + UUID.randomUUID().toString().replace("-", ""); }
}

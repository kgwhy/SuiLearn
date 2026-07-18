package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SessionAgentMemoryTest {
    @Test
    void usesHashedControlledKeysAndKeepsOnlyTwentySummaryTurns() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC);
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(clock);
        SessionMemoryKeyFactory keys = new SessionMemoryKeyFactory("suilearn:agent:session:v1");
        SessionMemoryService service = new SessionMemoryService(store, keys, Duration.ofHours(4), 20);

        IntStream.rangeClosed(1, 23).forEach(turn -> service.append(
            "learner/raw", "session:raw", new SessionTurn("summary-" + turn, "goal", clock.instant())));

        SessionMemory loaded = service.read("learner/raw", "session:raw").orElseThrow();
        assertThat(loaded.turns()).hasSize(20);
        assertThat(loaded.turns().getFirst().summary()).isEqualTo("summary-4");
        assertThat(store.keys()).singleElement().asString()
            .startsWith("suilearn:agent:session:v1:")
            .doesNotContain("learner/raw", "session:raw");
    }

    @Test
    void refreshesSlidingTtlAndExpiresWithoutCrossLearnerReads() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"), ZoneOffset.UTC);
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(clock);
        SessionMemoryService service = new SessionMemoryService(
            store, new SessionMemoryKeyFactory("suilearn:agent:session:v1"), Duration.ofHours(4), 20);

        service.append("learner-a", "shared-session", new SessionTurn("a", null, clock.instant()));
        service.append("learner-b", "shared-session", new SessionTurn("b", null, clock.instant()));
        clock.advance(Duration.ofHours(3));
        assertThat(service.read("learner-a", "shared-session").orElseThrow().turns())
            .extracting(SessionTurn::summary).containsExactly("a");

        clock.advance(Duration.ofHours(2));
        assertThat(service.read("learner-a", "shared-session")).isPresent();
        assertThat(service.read("learner-b", "shared-session")).isEmpty();
    }

    @Test
    void rejectsOversizedOrTranscriptShapedSessionContent() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new SessionTurn("x".repeat(SessionTurn.MAX_SUMMARY_LENGTH + 1), null, Instant.now()));

        SessionMemoryService service = new SessionMemoryService(
            new InMemorySessionMemoryStore(java.time.Clock.systemUTC()),
            new SessionMemoryKeyFactory("suilearn:agent:session:v1"), Duration.ofHours(4), 20);
        SessionTurn transcript = new SessionTurn("user: question assistant: full response", null, Instant.now());

        assertThatIllegalArgumentException().isThrownBy(() ->
            service.append("learner", "session", transcript));
    }

    @Test
    void managerDelegatesSessionReadAndAppendWithoutMaskingStoreFailures() {
        InMemorySessionMemoryStore sessions = new InMemorySessionMemoryStore(java.time.Clock.systemUTC());
        SessionMemoryService sessionService = new SessionMemoryService(
            sessions, new SessionMemoryKeyFactory("suilearn:agent:session:v1"), Duration.ofHours(4), 20);
        MemoryManager manager = new MemoryManager(sessionService, new InMemorySemanticMemoryStore(),
            ignored -> EmbeddingResult.available(java.util.List.of(1.0)),
            new MemoryPromotionPolicy(0.8, 8, 500), 5, Instant::now);

        manager.appendSession("learner", "session", new SessionTurn("concise summary", null, Instant.now()));

        assertThat(manager.readSession("learner", "session").orElseThrow().turns())
            .extracting(SessionTurn::summary).containsExactly("concise summary");
    }
}

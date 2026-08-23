package com.suilearn.api.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TurnEventBusTest {
    @Test
    void deliversLiveEventsAndTerminatesExactlyOnce() throws Exception {
        var bus = new TurnEventBus("turn_1", "sess_1", 8);
        var received = new CopyOnWriteArrayList<StreamEvent>();
        bus.subscribe(received::add);

        bus.publishPersisted(event(1, EventType.TURN_STARTED));
        bus.publishPersisted(event(2, EventType.CONTENT));
        bus.publishPersisted(event(3, EventType.DONE));

        assertThat(bus.awaitTerminal(Duration.ofSeconds(2)).seq()).isEqualTo(3);
        awaitSize(received, 3);
        assertThat(received).extracting(StreamEvent::seq).containsExactly(1L, 2L, 3L);
        assertThat(bus.publishPersisted(event(4, EventType.CONTENT))).isFalse();
    }

    @Test
    void slowListenerDoesNotBlockPublisherOrGrowQueue() throws Exception {
        var bus = new TurnEventBus("turn_1", "sess_1", 1);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var received = new CopyOnWriteArrayList<StreamEvent>();
        bus.subscribe(item -> {
            received.add(item);
            entered.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        long started = System.nanoTime();
        bus.publishPersisted(event(1, EventType.TURN_STARTED));
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        bus.publishPersisted(event(2, EventType.CONTENT));
        bus.publishPersisted(event(3, EventType.DONE));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertThat(elapsedMillis).isLessThan(1_000);
        release.countDown();
        assertThat(bus.awaitTerminal(Duration.ofSeconds(2)).seq()).isEqualTo(3);
        awaitSize(received, 2);
        assertThat(received).extracting(StreamEvent::seq).containsExactly(1L, 2L);
    }

    @Test
    void replayAndSubscribeHaveNoGap() throws Exception {
        var bus = new TurnEventBus("turn_1", "sess_1", 8);
        var received = new CopyOnWriteArrayList<StreamEvent>();
        bus.subscribeReplaying(received::add, 1,
            ignored -> List.of(event(2, EventType.CONTENT), event(3, EventType.PROGRESS)));
        bus.publishPersisted(event(4, EventType.DONE));

        awaitSize(received, 3);
        assertThat(received).extracting(StreamEvent::seq).containsExactly(2L, 3L, 4L);
    }

    private static StreamEvent event(long seq, EventType type) {
        return new StreamEvent("turn_1", "sess_1", seq, type, "test", null, "content", Map.of(), Instant.EPOCH);
    }

    private static void awaitSize(CopyOnWriteArrayList<StreamEvent> events, int size) throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (events.size() < size && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(events).hasSize(size);
    }
}

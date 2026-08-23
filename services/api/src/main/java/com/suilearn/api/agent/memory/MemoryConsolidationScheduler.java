package com.suilearn.api.agent.memory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryConsolidationScheduler {
    private final MemoryConsolidator consolidator;

    public MemoryConsolidationScheduler(MemoryConsolidator consolidator) {
        this.consolidator = consolidator;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void consolidateDueCommands() {
        consolidator.processDue();
    }
}

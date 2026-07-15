package com.suilearn.api.material.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

class LegacyMaterialRevisionMigrationRunnerTest {
    @Test
    void startupRunnerInvokesTheIdempotentLegacyRevisionMigration() throws Exception {
        var migrator = mock(LegacyMaterialRevisionMigrator.class);
        var runner = new LegacyMaterialRevisionMigrationRunner(migrator);

        runner.run(mock(ApplicationArguments.class));

        verify(migrator).migrateReadyLegacyMaterials();
    }
}

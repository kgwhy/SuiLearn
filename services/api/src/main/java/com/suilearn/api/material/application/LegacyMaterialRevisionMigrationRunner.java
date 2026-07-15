package com.suilearn.api.material.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Runs the idempotent legacy revision migration once at application startup. */
@Component
@Order(30)
public class LegacyMaterialRevisionMigrationRunner implements ApplicationRunner {
    private final LegacyMaterialRevisionMigrator migrator;

    public LegacyMaterialRevisionMigrationRunner(LegacyMaterialRevisionMigrator migrator) { this.migrator = migrator; }

    @Override public void run(ApplicationArguments args) { migrator.migrateReadyLegacyMaterials(); }
}

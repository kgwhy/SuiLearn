package com.suilearn.api.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationStoreBoundaryTest {
    @Test
    void applicationServicesDoNotDependOnLegacyStore() throws IOException {
        var sourceRoot = sourceRoot();

        try (var paths = Files.walk(sourceRoot)) {
            var offenders = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> path.toString().contains("\\application\\")
                    || path.toString().contains("/application/"))
                .filter(path -> containsLegacyStoreReference(path))
                .map(sourceRoot::relativize)
                .map(Path::toString)
                .sorted()
                .toList();

            assertThat(offenders).isEmpty();
        }
    }

    private boolean containsLegacyStoreReference(Path path) {
        try {
            return Files.readString(path).contains("SuiLearnV2Store");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private Path sourceRoot() {
        var serviceLocalRoot = Path.of("src/main/java");
        if (Files.exists(serviceLocalRoot)) {
            return serviceLocalRoot;
        }
        return Path.of("services/api/src/main/java");
    }
}

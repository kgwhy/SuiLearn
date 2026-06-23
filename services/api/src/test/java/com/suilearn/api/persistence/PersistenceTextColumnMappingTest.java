package com.suilearn.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PersistenceTextColumnMappingTest {
    @Test
    void persistenceEntitiesDoNotUseJpaLobMappings() throws IOException {
        var entityRoot = entityRoot();

        try (var paths = Files.walk(entityRoot)) {
            var offenders = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> containsJpaLob(path))
                .map(entityRoot::relativize)
                .map(Path::toString)
                .sorted()
                .toList();

            assertThat(offenders).isEmpty();
        }
    }

    private boolean containsJpaLob(Path path) {
        try {
            var source = Files.readString(path);
            return source.contains("jakarta.persistence.Lob") || source.contains("@Lob");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private Path entityRoot() {
        var serviceLocalRoot = Path.of("src/main/java/com/suilearn/api/persistence/entity");
        if (Files.exists(serviceLocalRoot)) {
            return serviceLocalRoot;
        }
        return Path.of("services/api/src/main/java/com/suilearn/api/persistence/entity");
    }
}

package com.suilearn.api.agent.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LearnerProfileServiceTest {
    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void savesAndReadsProfileAsBoundedJson() {
        var repo = mock(LearnerProfileJpaRepository.class);
        var service = new LearnerProfileService(repo, mapper, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        var profile = service.save("learner-a", "  curious  ", List.of(" Java ", "Spring"));

        assertThat(profile.persona()).isEqualTo("curious");
        assertThat(profile.skills()).containsExactly("Java", "Spring");
        var entity = ArgumentCaptor.forClass(LearnerProfileEntity.class);
        verify(repo).save(entity.capture());
        assertThat(entity.getValue().getSkillsJson()).isEqualTo("[\"Java\",\"Spring\"]");
        assertThat(entity.getValue().getPersona()).isEqualTo("curious");
    }

    @Test
    void rejectsOversizedProfile() {
        var repo = mock(LearnerProfileJpaRepository.class);
        var service = new LearnerProfileService(repo, mapper, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.save("learner-a", "x".repeat(2001), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("learner-a", "", java.util.Collections.nCopies(21, "skill")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

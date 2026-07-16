package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.suilearn.api.task.application.DeadLetterReplayService;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

class RuntimeFixtureControlTest {
    @Test
    void keepsTheTransactionalProbeServiceCglibProxyable() throws Exception {
        var trigger = RuntimeFixtureProbeService.class.getMethod("trigger", String.class);

        assertThat(trigger.getAnnotation(Transactional.class)).isNotNull();
        assertThat(Modifier.isFinal(RuntimeFixtureProbeService.class.getModifiers())).isFalse();
    }

    @Test
    void declaresTheProductionProbeConstructorAsTheSpringInjectionPoint() {
        var productionConstructors = Arrays.stream(RuntimeFixtureProbeService.class.getDeclaredConstructors())
            .filter(constructor -> Arrays.asList(constructor.getParameterTypes()).contains(TesseractOcrAdapter.class))
            .toList();

        assertThat(productionConstructors).hasSize(1);
        assertThat(productionConstructors.getFirst().getAnnotation(Autowired.class)).isNotNull();
    }

    @Test
    void declaresTheProductionConstructorAsTheSpringInjectionPoint() {
        var constructors = RuntimeFixtureController.class.getDeclaredConstructors();

        var productionConstructors = Arrays.stream(constructors)
            .filter(constructor -> constructor.getParameterCount() == 5)
            .toList();
        var testConstructors = Arrays.stream(constructors)
            .filter(constructor -> constructor.getParameterCount() == 3)
            .toList();

        assertThat(productionConstructors).hasSize(1);
        assertThat(productionConstructors.getFirst().getAnnotation(Autowired.class))
            .isNotNull();
        assertThat(testConstructors).hasSize(1);
        assertThat(testConstructors.getFirst().getAnnotation(Autowired.class))
            .isNull();
    }

    @Test
    void acceptsOnlyTheConfiguredTokenAndWhitelistedFaultModes() {
        var control = new RuntimeFixtureControl();
        var controller = new RuntimeFixtureController(control, mock(DeadLetterReplayService.class), "fixture-token");

        controller.setOcrMode("fixture-token", "TIMEOUT");
        controller.setAiMode("fixture-token", "TIMEOUT");

        assertThat(control.ocrMode()).isEqualTo(RuntimeFixtureControl.Mode.TIMEOUT);
        assertThat(control.aiMode()).isEqualTo(RuntimeFixtureControl.Mode.TIMEOUT);
        assertThatThrownBy(() -> controller.setOcrMode("wrong-token", "NORMAL"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> controller.setAiMode("fixture-token", "arbitrary"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void replaysOnlyTrackedDeadLetterIdentityWithoutAcceptingAPayload() {
        var deadLetters = mock(DeadLetterReplayService.class);
        var controller = new RuntimeFixtureController(new RuntimeFixtureControl(), deadLetters, "fixture-token");

        controller.replayDeadLetter("fixture-token", "message_1");

        verify(deadLetters).replay("message_1");
    }

    @Test
    void resetRestoresBothFaultModes() {
        var control = new RuntimeFixtureControl();
        control.setOcrMode(RuntimeFixtureControl.Mode.TIMEOUT);
        control.setAiMode(RuntimeFixtureControl.Mode.TIMEOUT);

        control.reset();

        assertThat(control.ocrMode()).isEqualTo(RuntimeFixtureControl.Mode.NORMAL);
        assertThat(control.aiMode()).isEqualTo(RuntimeFixtureControl.Mode.NORMAL);
    }

    @Test
    void resetEndpointRequiresTheFixtureTokenAndRestoresFaultModes() {
        var control = new RuntimeFixtureControl();
        control.setOcrMode(RuntimeFixtureControl.Mode.TIMEOUT);
        control.setAiMode(RuntimeFixtureControl.Mode.TIMEOUT);
        var controller = new RuntimeFixtureController(control, mock(DeadLetterReplayService.class), "fixture-token");

        controller.reset("fixture-token");

        assertThat(control.ocrMode()).isEqualTo(RuntimeFixtureControl.Mode.NORMAL);
        assertThat(control.aiMode()).isEqualTo(RuntimeFixtureControl.Mode.NORMAL);
        assertThatThrownBy(() -> controller.reset("wrong-token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN");
    }
}

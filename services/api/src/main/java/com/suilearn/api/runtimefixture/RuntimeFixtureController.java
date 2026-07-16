package com.suilearn.api.runtimefixture;

import com.suilearn.api.task.application.DeadLetterReplayService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Non-production fault controls; deliberately accepts no content, object key, credential, or broker payload. */
@RestController
@Profile("runtime-fixture")
@RequestMapping("/internal/runtime-fixture")
public final class RuntimeFixtureController {
    private static final String TOKEN_HEADER = "X-SuiLearn-Runtime-Fixture-Token";
    private final RuntimeFixtureControl control;
    private final DeadLetterReplayService deadLetters;
    private final RuntimeFixtureAiProvider aiProvider;
    private final RuntimeFixtureProbeService probes;
    private final String token;

    public RuntimeFixtureController(
        RuntimeFixtureControl control,
        DeadLetterReplayService deadLetters,
        @Value("${suilearn.runtime-fixture.token:}") String token
    ) {
        this(control, deadLetters, null, null, token);
    }

    @Autowired
    public RuntimeFixtureController(
        RuntimeFixtureControl control,
        DeadLetterReplayService deadLetters,
        RuntimeFixtureAiProvider aiProvider,
        RuntimeFixtureProbeService probes,
        @Value("${suilearn.runtime-fixture.token:}") String token
    ) {
        this.control = control;
        this.deadLetters = deadLetters;
        this.aiProvider = aiProvider;
        this.probes = probes;
        this.token = token == null ? "" : token;
    }

    @PutMapping("/ocr-mode")
    FixtureResponse setOcrMode(@RequestHeader(TOKEN_HEADER) String suppliedToken, @RequestParam String mode) {
        authorize(suppliedToken);
        control.setOcrMode(mode(mode));
        return FixtureResponse.ACCEPTED;
    }

    @PutMapping("/ai-mode")
    FixtureResponse setAiMode(@RequestHeader(TOKEN_HEADER) String suppliedToken, @RequestParam String mode) {
        authorize(suppliedToken);
        control.setAiMode(mode(mode));
        return FixtureResponse.ACCEPTED;
    }

    @PutMapping("/reset")
    FixtureResponse reset(@RequestHeader(TOKEN_HEADER) String suppliedToken) {
        authorize(suppliedToken);
        control.reset();
        if (aiProvider != null) aiProvider.resetCircuitBreaker();
        return FixtureResponse.ACCEPTED;
    }

    @PostMapping("/probes/{kind}")
    Object triggerProbe(
        @RequestHeader(TOKEN_HEADER) String suppliedToken, @PathVariable String kind
    ) {
        authorize(suppliedToken);
        if (probes == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Runtime fixture probes are unavailable");
        return probes.trigger(kind);
    }

    @PostMapping("/dlq/{messageId}/replay")
    FixtureResponse replayDeadLetter(@RequestHeader(TOKEN_HEADER) String suppliedToken, @PathVariable String messageId) {
        authorize(suppliedToken);
        if (!messageId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dead-letter identity");
        }
        deadLetters.replay(messageId);
        return FixtureResponse.ACCEPTED;
    }

    private void authorize(String suppliedToken) {
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        byte[] supplied = (suppliedToken == null ? "" : suppliedToken).getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Runtime fixture access denied");
        }
    }

    private RuntimeFixtureControl.Mode mode(String value) {
        try {
            return RuntimeFixtureControl.Mode.valueOf(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported runtime fixture mode");
        }
    }

    enum FixtureResponse { ACCEPTED }
}

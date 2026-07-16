package com.suilearn.api.runtimefixture;

import com.suilearn.api.material.document.ExternalProcessRunner;
import com.suilearn.api.material.document.RunningExternalProcess;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Supplies deterministic OCR timeout results without starting an external command. */
@Component
@Profile("runtime-fixture")
public final class RuntimeFixtureProcessRunner implements ExternalProcessRunner {
    private final RuntimeFixtureControl control;
    private final ExternalProcessRunner delegate;

    @Autowired
    public RuntimeFixtureProcessRunner(RuntimeFixtureControl control) {
        this(control, ExternalProcessRunner.processBuilder());
    }

    RuntimeFixtureProcessRunner(RuntimeFixtureControl control, ExternalProcessRunner delegate) {
        this.control = control;
        this.delegate = delegate;
    }

    @Override
    public RunningExternalProcess start(List<String> command) throws IOException {
        if (control.ocrMode() == RuntimeFixtureControl.Mode.TIMEOUT) {
            return TimedOutProcess.INSTANCE;
        }
        return delegate.start(command);
    }

    private enum TimedOutProcess implements RunningExternalProcess {
        INSTANCE;

        @Override public boolean await(Duration timeout) { return false; }
        @Override public int exitCode() { return 1; }
        @Override public String stdout() { return ""; }
        @Override public String stderr() { return ""; }
        @Override public void terminate() { }
    }
}

package com.suilearn.api.material.document;

import java.time.Duration;

/** A started external process whose output can be inspected after it finishes. */
public interface RunningExternalProcess {
    boolean await(Duration timeout) throws InterruptedException;

    int exitCode();

    String stdout();

    String stderr();

    void terminate();
}

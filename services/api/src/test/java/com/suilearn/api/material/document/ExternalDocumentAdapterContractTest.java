package com.suilearn.api.material.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * RED contract for Task 3.3. Production adapters are deliberately accessed by reflection so this
 * test compiles before their implementation exists. The process runner is injected and faked;
 * these tests never require Tesseract or LibreOffice to be installed.
 */
class ExternalDocumentAdapterContractTest {
    private static final String RUNNER_CLASS = "com.suilearn.api.material.document.ExternalProcessRunner";
    private static final String PROCESS_CLASS = "com.suilearn.api.material.document.RunningExternalProcess";
    private static final String OCR_CLASS = "com.suilearn.api.material.document.TesseractOcrAdapter";
    private static final String PREVIEW_CLASS = "com.suilearn.api.material.document.LibreOfficePreviewAdapter";

    @Test
    void exposesInjectableExternalProcessAndAdapterContracts() {
        assertThat(requiredType(RUNNER_CLASS)).isNotNull();
        assertThat(requiredType(PROCESS_CLASS)).isNotNull();
        assertThat(requiredType(OCR_CLASS)).isNotNull();
        assertThat(requiredType(PREVIEW_CLASS)).isNotNull();
    }

    @Test
    void tesseractTreatsHostileInputPathAsOneDataArgumentAndBuildsStablePageOperationKey() throws Exception {
        var runner = new RecordingRunner(ProcessBehavior.success("recognized text"));
        var adapter = tesseract(runner, 1, Duration.ofSeconds(2), "tesseract-v1");
        Path safePath = Path.of("C:/incoming/scan.png");
        Path hostilePath = Path.of("C:/incoming/scan;--psm 0 && erase-all.png");

        recognize(adapter, safePath, "revision-17", 2);
        var result = recognize(adapter, hostilePath, "revision-17", 3);

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.text()).isEqualTo("recognized text");
        assertThat(result.operationKey()).isEqualTo("ocr:revision-17:page-3:tesseract-v1");
        assertThat(commandShapeWithoutInput(runner.commands().get(1), hostilePath))
            .isEqualTo(commandShapeWithoutInput(runner.commands().get(0), safePath));
        assertThat(runner.commands().get(1)).contains(hostilePath.toString())
            .doesNotContain("sh", "-c", "cmd.exe", "/c");
    }

    @Test
    void tesseractProcessFailureIsReportedAsFailedInsteadOfReady() throws Exception {
        var runner = new RecordingRunner(ProcessBehavior.exited(7, "", "unreadable image"));
        var result = recognize(tesseract(runner, 1, Duration.ofSeconds(2), "tesseract-v1"),
            Path.of("C:/safe/scan.png"), "revision-17", 1);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.status()).isNotEqualTo("READY");
        assertThat(result.text()).isBlank();
    }

    @Test
    void tesseractTimeoutTerminatesTheExternalProcess() throws Exception {
        var runner = new RecordingRunner(ProcessBehavior.timedOut());
        var result = recognize(tesseract(runner, 1, Duration.ofMillis(50), "tesseract-v1"),
            Path.of("C:/safe/scan.png"), "revision-17", 1);

        assertThat(result.status()).isEqualTo("TIMED_OUT");
        assertThat(result.status()).isNotEqualTo("READY");
        assertThat(runner.terminatedCount()).isEqualTo(1);
    }

    @Test
    void tesseractSerializesOcrCallsWhenConfiguredConcurrencyIsOne() throws Exception {
        var releaseFirstProcess = new CountDownLatch(1);
        var runner = new RecordingRunner(ProcessBehavior.blocksUntil(releaseFirstProcess));
        var adapter = tesseract(runner, 1, Duration.ofSeconds(5), "tesseract-v1");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> recognize(adapter, Path.of("C:/safe/first.png"), "revision-17", 1));
            assertThat(runner.awaitFirstProcess(Duration.ofSeconds(1))).isTrue();

            var second = workers.submit(() -> recognize(adapter, Path.of("C:/safe/second.png"), "revision-17", 2));
            assertThat(runner.awaitSecondProcess(Duration.ofMillis(200))).isFalse();

            releaseFirstProcess.countDown();
            assertThat(first.get(1, TimeUnit.SECONDS).status()).isEqualTo("SUCCEEDED");
            assertThat(second.get(1, TimeUnit.SECONDS).status()).isEqualTo("SUCCEEDED");
            assertThat(runner.maxConcurrentProcesses()).isEqualTo(1);
        } finally {
            releaseFirstProcess.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void libreOfficePreviewFailurePreservesOriginalReferenceAndUsesDataSafeCommand() throws Exception {
        var runner = new RecordingRunner(ProcessBehavior.exited(1, "", "conversion failed"));
        var adapter = libreOffice(runner, Duration.ofSeconds(2), "libreoffice-v1");
        Path original = Path.of("C:/incoming/report;--headless && erase-all.docx");

        preview(adapter, Path.of("C:/incoming/report.docx"), "revision-16");
        var result = preview(adapter, original, "revision-17");

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.status()).isNotEqualTo("READY");
        assertThat(result.originalReference()).isEqualTo(original.toString());
        assertThat(result.previewReference()).isNull();
        assertThat(result.operationKey()).isEqualTo("preview:revision-17:libreoffice-v1");
        assertThat(commandShapeWithoutInput(runner.commands().get(1), original))
            .isEqualTo(commandShapeWithoutInput(runner.commands().get(0), Path.of("C:/incoming/report.docx")));
        assertThat(runner.commands().get(1)).contains(original.toString())
            .doesNotContain("sh", "-c", "cmd.exe", "/c");
        assertThat(runner.commands().get(1)).anyMatch(argument -> argument.startsWith("--outdir="));
        assertThat(runner.commands().get(1)).anyMatch(argument -> argument.startsWith("-env:UserInstallation=file:"));
        assertThat(runner.commands().get(1)).contains("--safe-mode");
    }

    @Test
    void processBackedRunnerTerminatesDescendantsBeforeItsParent() throws Exception {
        var process = Mockito.mock(Process.class);
        var parent = Mockito.mock(ProcessHandle.class);
        var child = Mockito.mock(ProcessHandle.class);
        Mockito.when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(process.toHandle()).thenReturn(parent);
        Mockito.when(parent.descendants()).thenReturn(java.util.stream.Stream.of(child));

        processBacked(process).terminate();

        InOrder order = Mockito.inOrder(child, process);
        order.verify(child).destroyForcibly();
        order.verify(process).destroyForcibly();
    }

    @Test
    void processBackedRunnerFailsWhenStdoutExceedsItsBound() throws Exception {
        var process = Mockito.mock(Process.class);
        Mockito.when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1_048_577]));
        Mockito.when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(process.waitFor(Mockito.anyLong(), Mockito.any())).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processBacked(process).await(Duration.ofSeconds(1)))
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("output exceeds");
    }

    @Test
    void processBackedRunnerFailsWhenStderrExceedsItsBound() throws Exception {
        var process = Mockito.mock(Process.class);
        Mockito.when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[1_048_577]));
        Mockito.when(process.waitFor(Mockito.anyLong(), Mockito.any())).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processBacked(process).await(Duration.ofSeconds(1)))
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("output exceeds");
    }

    private static RunningExternalProcess processBacked(Process process) throws Exception {
        var type = Class.forName("com.suilearn.api.material.document.ExternalProcessRunner$ProcessBackedExternalProcess");
        var constructor = type.getDeclaredConstructor(Process.class);
        constructor.setAccessible(true);
        return (RunningExternalProcess) constructor.newInstance(process);
    }

    private static Object tesseract(RecordingRunner runner, int concurrency, Duration timeout, String adapterVersion)
        throws Exception {
        Class<?> runnerType = requiredType(RUNNER_CLASS);
        Class<?> type = requiredType(OCR_CLASS);
        return type.getConstructor(String.class, runnerType, int.class, Duration.class, String.class)
            .newInstance("tesseract", runner.proxy(runnerType), concurrency, timeout, adapterVersion);
    }

    private static Object libreOffice(RecordingRunner runner, Duration timeout, String adapterVersion) throws Exception {
        Class<?> runnerType = requiredType(RUNNER_CLASS);
        Class<?> type = requiredType(PREVIEW_CLASS);
        return type.getConstructor(String.class, runnerType, Duration.class, String.class)
            .newInstance("soffice", runner.proxy(runnerType), timeout, adapterVersion);
    }

    private static OcrResult recognize(Object adapter, Path input, String revisionId, int pageNumber) throws Exception {
        Object value = adapter.getClass().getMethod("recognize", Path.class, String.class, int.class)
            .invoke(adapter, input, revisionId, pageNumber);
        return new OcrResult(
            (String) invoke(value, "status"),
            (String) invoke(value, "text"),
            (String) invoke(value, "operationKey")
        );
    }

    private static PreviewResult preview(Object adapter, Path original, String revisionId) throws Exception {
        Object value = adapter.getClass().getMethod("preview", Path.class, String.class).invoke(adapter, original, revisionId);
        return new PreviewResult(
            (String) invoke(value, "status"),
            (String) invoke(value, "originalReference"),
            (String) invoke(value, "previewReference"),
            (String) invoke(value, "operationKey")
        );
    }

    private static Class<?> requiredType(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Task 3.3 must provide " + className, exception);
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Task 3.3 result is missing " + methodName + "()", exception);
        }
    }

    private static List<String> commandShapeWithoutInput(List<String> command, Path input) {
        return command.stream().map(argument -> {
            if (argument.equals(input.toString())) return "<input>";
            if (argument.startsWith("--outdir=")) return "--outdir=<unique>";
            if (argument.startsWith("-env:UserInstallation=file:")) return "-env:UserInstallation=<unique>";
            return argument;
        }).toList();
    }

    private record OcrResult(String status, String text, String operationKey) {
    }

    private record PreviewResult(String status, String originalReference, String previewReference, String operationKey) {
    }

    private record ProcessBehavior(boolean completesBeforeTimeout, int exitCode, String stdout, String stderr,
                                   CountDownLatch release) {
        static ProcessBehavior success(String stdout) {
            return exited(0, stdout, "");
        }

        static ProcessBehavior exited(int exitCode, String stdout, String stderr) {
            return new ProcessBehavior(true, exitCode, stdout, stderr, null);
        }

        static ProcessBehavior timedOut() {
            return new ProcessBehavior(false, -1, "", "", null);
        }

        static ProcessBehavior blocksUntil(CountDownLatch release) {
            return new ProcessBehavior(true, 0, "recognized text", "", release);
        }
    }

    private static final class RecordingRunner implements InvocationHandler {
        private final ProcessBehavior behavior;
        private final List<List<String>> commands = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger running = new AtomicInteger();
        private final AtomicInteger maxRunning = new AtomicInteger();
        private final AtomicInteger terminated = new AtomicInteger();
        private final CountDownLatch firstProcess = new CountDownLatch(1);
        private final CountDownLatch secondProcess = new CountDownLatch(1);

        private RecordingRunner(ProcessBehavior behavior) {
            this.behavior = behavior;
        }

        Object proxy(Class<?> runnerType) {
            return Proxy.newProxyInstance(runnerType.getClassLoader(), new Class<?>[] {runnerType}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getName().equals("start")) {
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) arguments[0];
                commands.add(List.copyOf(command));
                int started = commands.size();
                if (started == 1) {
                    firstProcess.countDown();
                } else if (started == 2) {
                    secondProcess.countDown();
                }
                return processProxy(requiredType(PROCESS_CLASS));
            }
            if (method.getName().equals("toString")) {
                return "recording-process-runner";
            }
            throw new AssertionError("Unexpected process runner method: " + method);
        }

        private Object processProxy(Class<?> processType) {
            return Proxy.newProxyInstance(processType.getClassLoader(), new Class<?>[] {processType}, (proxy, method, arguments) -> {
                return switch (method.getName()) {
                    case "await" -> await();
                    case "exitCode" -> behavior.exitCode();
                    case "stdout" -> behavior.stdout();
                    case "stderr" -> behavior.stderr();
                    case "terminate" -> {
                        terminated.incrementAndGet();
                        yield null;
                    }
                    case "toString" -> "recording-process";
                    default -> throw new AssertionError("Unexpected running process method: " + method);
                };
            });
        }

        private boolean await() throws InterruptedException {
            int concurrent = running.incrementAndGet();
            maxRunning.accumulateAndGet(concurrent, Math::max);
            try {
                if (behavior.release() != null) {
                    behavior.release().await(1, TimeUnit.SECONDS);
                }
                return behavior.completesBeforeTimeout();
            } finally {
                running.decrementAndGet();
            }
        }

        List<List<String>> commands() {
            return List.copyOf(commands);
        }

        int terminatedCount() {
            return terminated.get();
        }

        int maxConcurrentProcesses() {
            return maxRunning.get();
        }

        boolean awaitFirstProcess(Duration timeout) throws InterruptedException {
            return firstProcess.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean awaitSecondProcess(Duration timeout) throws InterruptedException {
            return secondProcess.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}

package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.SplatConversionPort;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Converts Gaussian-splat files between formats via the GaussForge {@code gfconvert} native CLI. The tool
 * is extension-driven ({@code gfconvert <in> <out.ext>}), so the output filename carries the target
 * format's extension. Sole writer of {@code .ksplat} in the toolkit.
 *
 * <p>Follows the {@code BlenderProcessIntegration} subprocess discipline: stdout is drained on a separate
 * thread before {@code waitFor} to avoid a pipe-buffer deadlock, a timeout guards against hangs, and a
 * non-zero exit throws {@link KnownRuntimeError} after logging stderr.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GaussForgeSplatConverter implements SplatConversionPort {

    private static final long PROCESS_TIMEOUT_MINUTES = 15;

    private final FileStoragePort fileStorage;

    @Override
    public File convert(File input, SplatFormat target) throws Exception {
        File output = new File(
                fileStorage.getOutputDirectory(),
                OutputFileNaming.baseName(input.getName()) + "_converted_" + OutputFileNaming.timestamp()
                        + target.extension());
        Process process = getProcessBuilder(input.getAbsolutePath(), output.getAbsolutePath())
                .start();
        CompletableFuture<Void> stdoutFuture =
                CompletableFuture.runAsync(() -> withReturnReader(process.getInputStream(), stream -> {
                    stream.forEach(line -> log.debug("[gfconvert stdout] {}", line));
                    return null;
                }));
        try {
            if (!process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new KnownRuntimeError("gfconvert timed out converting: " + input.getName());
            }
            stdoutFuture.join();
            if (process.exitValue() != 0) {
                String stderr =
                        withReturnReader(process.getErrorStream(), stream -> stream.collect(Collectors.joining("\n")));
                log.error("gfconvert failed (exit {}) for {}: {}", process.exitValue(), input.getName(), stderr);
                throw new KnownRuntimeError("gfconvert failed to convert " + input.getName() + " to " + target);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new KnownRuntimeError("gfconvert process was interrupted for: " + input.getName(), e);
        }
        return output;
    }

    private ProcessBuilder getProcessBuilder(String... args) {
        String osName = System.getProperty("os.name");
        List<String> cmd = new ArrayList<>(List.of(args));
        cmd.addFirst((osName != null && osName.contains("Windows")) ? "gfconvert.exe" : "gfconvert");
        return new ProcessBuilder(cmd);
    }

    private <R> R withReturnReader(InputStream inputStream, StreamHandlerWithReturnValue<R> handler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return handler.handle(reader.lines());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface StreamHandlerWithReturnValue<R> {
        R handle(Stream<String> lines) throws Exception;
    }
}

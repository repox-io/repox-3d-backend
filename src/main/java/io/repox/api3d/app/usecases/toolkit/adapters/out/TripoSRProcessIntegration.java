package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.toolkit.application.ports.out.TripoSRProcessPort;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.TripoSRProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripoSRProcessIntegration implements TripoSRProcessPort {

    private final TripoSRProperties tripoSRProperties;

    @FunctionalInterface
    interface StreamHandlerWithReturnValue<R> {
        R handle(Stream<String> lines) throws Exception;
    }

    private <R> R withReturnReader(InputStream inputStream, StreamHandlerWithReturnValue<R> handler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return handler.handle(reader.lines());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File convert(File imageFile, File outputDir) throws Exception {
        String runScript = Path.of(tripoSRProperties.getDir(), "run.py").toString();
        Process process = new ProcessBuilder(
                        tripoSRProperties.getPython(),
                        runScript,
                        imageFile.getAbsolutePath(),
                        "--output-dir",
                        outputDir.getAbsolutePath(),
                        "--device",
                        tripoSRProperties.getDevice(),
                        "--model-save-format",
                        tripoSRProperties.getSaveFormat())
                .redirectErrorStream(false)
                .start();

        CompletableFuture<Void> stdoutDrain =
                CompletableFuture.runAsync(() -> withReturnReader(process.getInputStream(), stream -> {
                    stream.forEach(line -> log.debug("[triposr] {}", line));
                    return null;
                }));

        try {
            int exitCode = process.waitFor();
            stdoutDrain.join();
            if (exitCode != 0) {
                String stderr =
                        withReturnReader(process.getErrorStream(), stream -> stream.collect(Collectors.joining("\n")));
                log.error("TripoSR process failed (exit {}): {}", exitCode, stderr);
                throw new KnownRuntimeError("TripoSR failed to convert image: " + imageFile.getName());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new KnownRuntimeError("TripoSR process was interrupted for: " + imageFile.getName(), e);
        }

        return findGlbOutput(outputDir, imageFile.getName());
    }

    private File findGlbOutput(File outputDir, String imageName) throws IOException {
        try (Stream<Path> files = Files.walk(outputDir.toPath())) {
            return files.filter(p -> p.toString().endsWith("." + tripoSRProperties.getSaveFormat()))
                    .findFirst()
                    .map(Path::toFile)
                    .orElseThrow(() -> new KnownRuntimeError(
                            "TripoSR produced no ." + tripoSRProperties.getSaveFormat() + " output for: " + imageName));
        }
    }
}

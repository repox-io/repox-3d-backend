package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.AssimpProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpExportFormatEnum;
import io.repox.api3d.app.usecases.toolkit.domain.AssimpInfoTechnicalDetails;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssimpProcessIntegration implements AssimpProcessPort {

    private static final long PROCESS_TIMEOUT_MINUTES = 15;

    private final FileStoragePort fileStorage;

    @FunctionalInterface
    interface StreamHandler {
        void handle(Stream<String> lines) throws Exception;
    }

    void withReader(InputStream inputStream, StreamHandler handler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            handler.handle(reader.lines());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @FunctionalInterface
    interface StreamHandlerWithReturnValue<R> {
        R handle(Stream<String> lines) throws Exception;
    }

    <R> R withReturnReader(InputStream inputStream, StreamHandlerWithReturnValue<R> handler) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return handler.handle(reader.lines());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AssimpInfoTechnicalDetails info(File file) throws IOException {
        Process assimp = getAssimpProcessBuilder("info", file.getAbsolutePath(), "--raw", "--silent")
                .start();
        List<Long> orderedLineValues = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        output.append("Assimp processing output:\n");
        // Drain stdout asynchronously so a stalled process is bounded by the timeout below rather than
        // blocking this thread forever on a read that never reaches EOF.
        CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(() -> withReader(
                assimp.getInputStream(),
                (stream) -> stream.forEach(line -> {
                    AssimpInfoTechnicalDetails.collectLineValues(line, orderedLineValues);
                    output.append(line).append("\n");
                })));
        int exitCode;
        try {
            if (!assimp.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                assimp.destroyForcibly();
                throw new KnownRuntimeError("Assimp info timed out for: " + file.getName());
            }
            stdoutFuture.join();
            exitCode = assimp.exitValue();
        } catch (InterruptedException e) {
            assimp.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new KnownRuntimeError("Assimp info was interrupted for: " + file.getName(), e);
        }
        log.trace(output.toString());
        if (exitCode != 0) {
            log.warn("Assimp info exited {} for {}", exitCode, file.getName());
        }
        if (orderedLineValues.isEmpty()) {
            String errorLine = output.toString()
                    .lines()
                    .filter(line -> line.contains("ERROR"))
                    .map(line -> line.substring(line.indexOf("ERROR")))
                    .collect(Collectors.joining("\n"));
            throw new RuntimeException("Assimp could not produce info (exit " + exitCode + "). "
                    + (errorLine.isBlank() ? output.toString() : errorLine));
        }
        return AssimpInfoTechnicalDetails.fromOrdererValuesList(orderedLineValues);
    }

    @Override
    public List<String> listext() throws IOException {
        Process assimp = getAssimpProcessBuilder("listext").start();
        return withReturnReader(assimp.getInputStream(), (stream) -> stream.map(line -> Arrays.asList(line.split(";")))
                .flatMap(List::stream)
                .toList());
    }

    @Override
    public boolean knownext(File file) throws IOException {
        String ext = StringUtils.getFilenameExtension(file.getName());
        if (org.apache.commons.lang3.StringUtils.isBlank(ext)) {
            throw new UnsupportedOperationException("Invalid file type.");
        }
        Process assimp = getAssimpProcessBuilder("knowext", ext).start();
        return withReturnReader(
                assimp.getInputStream(),
                (stream) -> stream.anyMatch(line -> line.contains(ext) && line.contains("is known")));
    }

    /**
     * Exports a given file to an AssimpExportFormatEnum format. The exported file is written to the shared
     * output directory, along with any other assimp-produced sidecar files for the export flow (i.e. mtl
     * files, texture files etc.).
     **/
    @Override
    public File export(File file, AssimpExportFormatEnum exportFormat) throws IOException {
        File output = Path.of(
                        fileStorage.getOutputDirectory().getAbsolutePath(),
                        OutputFileNaming.baseName(file.getName()) + "_converted_" + OutputFileNaming.timestamp()
                                + exportFormat.toFormatExtension())
                .toFile();
        Process assimp = getAssimpProcessBuilder(
                        "export", file.getAbsolutePath(), output.getAbsolutePath(), "-f", exportFormat.value)
                .start();
        // Drain stdout asynchronously to prevent buffer deadlock.
        CompletableFuture<Void> stdoutFuture =
                CompletableFuture.runAsync(() -> withReturnReader(assimp.getInputStream(), stream -> {
                    stream.forEach(line -> log.debug("[assimp stdout] {}", line));
                    return null;
                }));
        try {
            if (!assimp.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                assimp.destroyForcibly();
                throw new KnownRuntimeError("Assimp export timed out for: " + file.getName());
            }
            stdoutFuture.join();
            if (assimp.exitValue() != 0) {
                String stderr =
                        withReturnReader(assimp.getErrorStream(), (stream) -> stream.collect(Collectors.joining("\n")));
                log.error("Assimp export failed (exit {}) for {}: {}", assimp.exitValue(), file.getName(), stderr);
                throw new KnownRuntimeError(
                        "Assimp failed to export " + file.getName() + " to " + exportFormat.value + ": " + stderr);
            }
        } catch (InterruptedException e) {
            assimp.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new KnownRuntimeError("Assimp export was interrupted for: " + file.getName(), e);
        }
        return output;
    }

    @Override
    public List<String> listexport() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String exportinfo(String format) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public List<Object> extract(File file) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Object dump(File file) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Object cmpdump(List<File> dumps) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String version() throws IOException {
        Process assimp = getAssimpProcessBuilder("version").start();
        return "Using Open Asset Import Library (\"Assimp\") "
                + withReturnReader(assimp.getInputStream(), (stream) -> stream.filter(line -> line.contains("Version"))
                        .map(String::valueOf)
                        .findFirst()
                        .orElse(null));
    }

    private ProcessBuilder getAssimpProcessBuilder(String... args) {
        String osName = System.getProperty("os.name");
        List<String> cmd = new ArrayList<>(List.of(args));
        if (osName.contains("Windows")) {
            cmd.addFirst("assimpd.exe");
            return new ProcessBuilder(cmd);
        }
        cmd.addFirst("assimp");
        return new ProcessBuilder(cmd);
    }
}

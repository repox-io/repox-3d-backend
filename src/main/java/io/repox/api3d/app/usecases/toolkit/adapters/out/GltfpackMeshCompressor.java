package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.MeshCompressionPort;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
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
 * Compresses glTF/GLB meshes for delivery via the gltfpack (meshoptimizer) native CLI: meshopt geometry
 * ({@code -c}/{@code -cc}) plus optional KTX2/Basis texture supercompression ({@code -tc}) and mesh
 * simplification ({@code -si}). Output is always a self-contained {@code .glb}.
 *
 * <p>Follows the {@code BlenderProcessIntegration} subprocess discipline: async stdout drain before
 * {@code waitFor}, a timeout, and {@link KnownRuntimeError} on non-zero exit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GltfpackMeshCompressor implements MeshCompressionPort {

    private static final long PROCESS_TIMEOUT_MINUTES = 15;

    private final FileStoragePort fileStorage;

    @Override
    public File compress(File input, MeshCompressionOptions options) throws Exception {
        File output = new File(
                fileStorage.getOutputDirectory(),
                OutputFileNaming.baseName(input.getName()) + "_compressed_" + OutputFileNaming.timestamp() + ".glb");

        List<String> args = new ArrayList<>(List.of("-i", input.getAbsolutePath(), "-o", output.getAbsolutePath()));
        args.add(options.aggressive() ? "-cc" : "-c");
        if (options.compressTextures()) {
            args.add("-tc");
        }
        if (options.simplifyRatio() != null) {
            args.add("-si");
            args.add(Float.toString(options.simplifyRatio()));
        }

        Process process = getProcessBuilder(args.toArray(String[]::new)).start();
        CompletableFuture<Void> stdoutFuture =
                CompletableFuture.runAsync(() -> withReturnReader(process.getInputStream(), stream -> {
                    stream.forEach(line -> log.debug("[gltfpack stdout] {}", line));
                    return null;
                }));
        try {
            if (!process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new KnownRuntimeError("gltfpack timed out compressing: " + input.getName());
            }
            stdoutFuture.join();
            if (process.exitValue() != 0) {
                String stderr =
                        withReturnReader(process.getErrorStream(), stream -> stream.collect(Collectors.joining("\n")));
                log.error("gltfpack failed (exit {}) for {}: {}", process.exitValue(), input.getName(), stderr);
                throw new KnownRuntimeError("gltfpack failed to compress: " + input.getName());
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new KnownRuntimeError("gltfpack process was interrupted for: " + input.getName(), e);
        }
        return output;
    }

    private ProcessBuilder getProcessBuilder(String... args) {
        String osName = System.getProperty("os.name");
        List<String> cmd = new ArrayList<>(List.of(args));
        cmd.addFirst((osName != null && osName.contains("Windows")) ? "gltfpack.exe" : "gltfpack");
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

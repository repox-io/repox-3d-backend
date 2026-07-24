package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.BlenderProcessPort;
import io.repox.api3d.app.usecases.toolkit.domain.BlenderExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.options.BlenderProperties;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class BlenderProcessIntegration implements BlenderProcessPort {

    private static final long PROCESS_TIMEOUT_MINUTES = 15;

    private final BlenderProperties blenderProperties;
    private final FileStoragePort fileStorage;

    private String resolvedThumbnailScriptPath;
    private String resolvedInfoScriptPath;
    private String resolvedConvertScriptPath;
    private String resolvedUsdzOptimizeScriptPath;

    @PostConstruct
    void resolveScriptPaths() throws IOException {
        resolvedThumbnailScriptPath = resolveScriptPath(blenderProperties.getThumbnailScript());
        log.info("Blender thumbnail script resolved to: {}", resolvedThumbnailScriptPath);
        resolvedInfoScriptPath = resolveScriptPath(blenderProperties.getInfoScript());
        log.info("Blender info script resolved to: {}", resolvedInfoScriptPath);
        resolvedConvertScriptPath = resolveScriptPath(blenderProperties.getConvertScript());
        log.info("Blender convert script resolved to: {}", resolvedConvertScriptPath);
        resolvedUsdzOptimizeScriptPath = resolveScriptPath(blenderProperties.getUsdzOptimizeScript());
        log.info("Blender usdz-optimize script resolved to: {}", resolvedUsdzOptimizeScriptPath);
    }

    /**
     * Resolves the configured script path to an absolute filesystem path.
     * classpath: paths are extracted to a temp file so they work inside a fat JAR.
     * Plain filesystem paths are used as-is.
     */
    private String resolveScriptPath(String configuredPath) throws IOException {
        if (!configuredPath.startsWith("classpath:")) {
            return configuredPath;
        }
        String classpathLocation = configuredPath.substring("classpath:".length());
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Blender script not found in classpath: " + configuredPath);
        }
        Path tempScript = Files.createTempFile("thumb_render_", ".py");
        tempScript.toFile().deleteOnExit();
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, tempScript, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempScript.toAbsolutePath().toString();
    }

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
    public File thumbnail(File modelFile) throws Exception {
        File output = new File(
                fileStorage.getOutputDirectory(),
                OutputFileNaming.baseName(modelFile.getName()) + "_thumbnail_" + OutputFileNaming.timestamp() + ".png");
        runBlenderScript(
                modelFile,
                "render thumbnail",
                resolvedThumbnailScriptPath,
                modelFile.getAbsolutePath(),
                output.getAbsolutePath());
        // Blender can exit 0 yet render nothing (e.g. an unsupported/empty source). Fail loudly here so the
        // job is marked FAILED with a clear message rather than completing with an output that isn't on disk.
        if (!output.isFile()) {
            throw new KnownRuntimeError("Blender produced no thumbnail for: " + modelFile.getName());
        }
        return output;
    }

    @Override
    public ModelTechnicalDetailsDto info(File modelFile) throws Exception {
        Map<String, Long> metrics = new HashMap<>();
        runBlenderScript(
                modelFile,
                "extract info",
                line -> collectMetric(line, metrics),
                resolvedInfoScriptPath,
                modelFile.getAbsolutePath());
        return new ModelTechnicalDetailsDto(
                null, // memoryConsumptionB — no Blender equivalent
                metrics.get("nodes"),
                metrics.get("meshes"),
                metrics.get("animations"),
                metrics.get("textures"),
                metrics.get("materials"),
                metrics.get("cameras"),
                metrics.get("lights"),
                metrics.get("vertices"),
                metrics.get("faces"),
                metrics.get("bones"),
                metrics.get("animationChannels"),
                null, // pointCount — meshes have no point/gaussian concept
                null); // gaussianCount
    }

    /**
     * Parses a {@code MODELINFO:key=value} line emitted by model_info.py into the metrics map.
     */
    private void collectMetric(String line, Map<String, Long> metrics) {
        if (!line.startsWith("MODELINFO:")) {
            return;
        }
        String[] keyValue = line.substring("MODELINFO:".length()).split("=", 2);
        if (keyValue.length != 2) {
            return;
        }
        try {
            metrics.put(keyValue[0].trim(), Long.parseLong(keyValue[1].trim()));
        } catch (NumberFormatException e) {
            log.warn("Could not parse Blender info metric: {}", line);
        }
    }

    @Override
    public File convert(File modelFile, BlenderExportFormat target) throws Exception {
        File output = new File(
                fileStorage.getOutputDirectory(),
                OutputFileNaming.baseName(modelFile.getName()) + "_blender_" + OutputFileNaming.timestamp()
                        + target.extension());
        runBlenderScript(
                modelFile,
                "convert to " + target,
                resolvedConvertScriptPath,
                modelFile.getAbsolutePath(),
                output.getAbsolutePath(),
                target.token());
        return output;
    }

    @Override
    public File optimizeUsdz(File modelFile, UsdzOptimizeOptions options) throws Exception {
        File output = new File(
                fileStorage.getOutputDirectory(),
                OutputFileNaming.baseName(modelFile.getName()) + "_optimized_" + OutputFileNaming.timestamp()
                        + ".usdz");
        runBlenderScript(
                modelFile,
                "optimize usdz",
                resolvedUsdzOptimizeScriptPath,
                modelFile.getAbsolutePath(),
                output.getAbsolutePath(),
                options.decimateRatio() == null ? "" : Float.toString(options.decimateRatio()),
                options.textureMaxSize() == null ? "" : Integer.toString(options.textureMaxSize()));
        return output;
    }

    /**
     * Runs a background Blender Python script ({@code blender --background --python <script> -- <args>})
     * with the shared subprocess discipline: async stdout drain, timeout with forcible destroy, and
     * {@link KnownRuntimeError} on non-zero exit.
     */
    private void runBlenderScript(File modelFile, String action, String scriptPath, String... scriptArgs)
            throws Exception {
        runBlenderScript(modelFile, action, line -> {}, scriptPath, scriptArgs);
    }

    /**
     * Variant of {@link #runBlenderScript(File, String, String, String...)} for scripts whose stdout carries
     * results: {@code onStdoutLine} is invoked for every line as it is drained.
     */
    private void runBlenderScript(
            File modelFile, String action, Consumer<String> onStdoutLine, String scriptPath, String... scriptArgs)
            throws Exception {
        List<String> args = new ArrayList<>(List.of("--background", "--python", scriptPath, "--"));
        args.addAll(List.of(scriptArgs));
        Process blender = getBlenderProcessBuilder(args.toArray(String[]::new)).start();
        CompletableFuture<Void> stdoutFuture =
                CompletableFuture.runAsync(() -> withReturnReader(blender.getInputStream(), stream -> {
                    stream.forEach(line -> {
                        log.debug("[blender stdout] {}", line);
                        onStdoutLine.accept(line);
                    });
                    return null;
                }));
        try {
            if (!blender.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                blender.destroyForcibly();
                throw new KnownRuntimeError("Blender " + action + " timed out for: " + modelFile.getName());
            }
            stdoutFuture.join();
            if (blender.exitValue() != 0) {
                String stderr =
                        withReturnReader(blender.getErrorStream(), stream -> stream.collect(Collectors.joining("\n")));
                log.error("Blender {} failed (exit {}): {}", action, blender.exitValue(), stderr);
                throw new KnownRuntimeError("Blender failed to " + action + " for: " + modelFile.getName());
            }
        } catch (InterruptedException e) {
            blender.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new KnownRuntimeError("Blender " + action + " was interrupted for: " + modelFile.getName(), e);
        }
    }

    @Override
    public String version() throws IOException {
        Process blender = getBlenderProcessBuilder("--version").start();
        return withReturnReader(blender.getInputStream(), stream -> stream.filter(line -> line.startsWith("Blender"))
                .findFirst()
                .orElse("Blender version unknown"));
    }

    private ProcessBuilder getBlenderProcessBuilder(String... args) {
        String osName = System.getProperty("os.name");
        List<String> cmd = new ArrayList<>(List.of(args));
        if (osName != null && osName.contains("Windows")) {
            cmd.addFirst("blender.exe");
        } else {
            cmd.addFirst("blender");
            cmd.addFirst("-a");
            cmd.addFirst("xvfb-run");
        }
        return new ProcessBuilder(cmd);
    }
}

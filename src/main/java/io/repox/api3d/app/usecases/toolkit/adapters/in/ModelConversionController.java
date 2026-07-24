package io.repox.api3d.app.usecases.toolkit.adapters.in;

import static io.repox.api3d.app.usecases.job.application.JobResponses.accepted;

import io.repox.api3d.app.usecases.job.application.JobStatusDto;
import io.repox.api3d.app.usecases.job.application.ports.in.ProcessingJobUseCase;
import io.repox.api3d.app.usecases.job.domain.ModelProcessingJobType;
import io.repox.api3d.app.usecases.toolkit.domain.MeshCompressionOptions;
import io.repox.api3d.app.usecases.toolkit.domain.MeshExportFormat;
import io.repox.api3d.app.usecases.toolkit.domain.SplatFormat;
import io.repox.api3d.app.usecases.toolkit.domain.UsdzOptimizeOptions;
import io.repox.api3d.app.utils.ModelSourceUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import io.repox.api3d.appconfig.OpenApiTagConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Submits 3D-model conversion/compression as asynchronous jobs. Each endpoint accepts the source model as
 * either a multipart {@code file} upload or a {@code url} parameter (exactly one), enqueues work and returns
 * {@code 202 Accepted} with a {@link JobStatusDto} (and a {@code Location} header to the polling endpoint);
 * the client then polls {@code GET /jobs/{id}} and downloads the result from {@code GET /jobs/{id}/output}.
 */
@Tag(name = OpenApiTagConfig.API_3D_TOOLKIT)
@RestController
@RequiredArgsConstructor
public class ModelConversionController {

    private final ProcessingJobUseCase processingJobUseCase;

    /** Default target for /tools/compress/splat when the caller omits one — a modern, compact container. */
    private static final SplatFormat DEFAULT_SPLAT_COMPRESSION_FORMAT = SplatFormat.SPZ;

    // ── Splat conversion (GaussForge) ─────────────────────────────────────────
    @Operation(summary = "Submit a job to convert a Gaussian-splat file (upload or URL) to another splat format.")
    @PostMapping(value = "/tools/convert/splat")
    ResponseEntity<JobStatusDto> convertSplat(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestParam SplatFormat targetFormat) {
        return accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(
                                ModelProcessingJobType.SPLAT_CONVERT, file, targetFormat)
                        : processingJobUseCase.submit(ModelProcessingJobType.SPLAT_CONVERT, url, targetFormat));
    }

    // ── Mesh conversion (Assimp, Blender fallback) ────────────────────────────
    @Operation(summary = "Submit a job to convert a mesh (upload or URL) to another format (Assimp, Blender fallback).")
    @PostMapping(value = "/tools/convert/mesh")
    ResponseEntity<JobStatusDto> convertMesh(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestParam MeshExportFormat targetFormat) {
        return accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.MESH_CONVERT, file, targetFormat)
                        : processingJobUseCase.submit(ModelProcessingJobType.MESH_CONVERT, url, targetFormat));
    }

    // ── Mesh compression (gltfpack) ───────────────────────────────────────────
    @Operation(
            summary = "Submit a job to compress a glTF/GLB mesh (upload or URL) for delivery (gltfpack; meshopt"
                    + " output). Draco-compressed inputs are rejected with 400.")
    @PostMapping(value = "/tools/compress/mesh")
    ResponseEntity<JobStatusDto> compressMesh(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "true") boolean compressTextures,
            @RequestParam(defaultValue = "false") boolean aggressive,
            @RequestParam(required = false) Float simplifyRatio) {
        MeshCompressionOptions options = new MeshCompressionOptions(compressTextures, aggressive, simplifyRatio);
        return accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.MESH_COMPRESS, file, options)
                        : processingJobUseCase.submit(ModelProcessingJobType.MESH_COMPRESS, url, options));
    }

    // ── USDZ compression (Blender optimize) ───────────────────────────────────
    @Operation(
            summary = "Submit a job to optimize/compress a USDZ file (upload or URL) (Blender decimate + downscale).")
    @PostMapping(value = "/tools/compress/usdz")
    ResponseEntity<JobStatusDto> compressUsdz(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) Float decimateRatio,
            @RequestParam(required = false) Integer textureMaxSize) {
        UsdzOptimizeOptions options = new UsdzOptimizeOptions(decimateRatio, textureMaxSize);
        return accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.USDZ_COMPRESS, file, options)
                        : processingJobUseCase.submit(ModelProcessingJobType.USDZ_COMPRESS, url, options));
    }

    // ── Splat compression (GaussForge — compact target only) ──────────────────
    @Operation(
            summary = "Submit a job to compress a Gaussian-splat file (upload or URL) (GaussForge). Target must be a"
                    + " compact format (SPZ, SOG, KSPLAT, COMPRESSED_PLY); defaults to SPZ.")
    @PostMapping(value = "/tools/compress/splat")
    ResponseEntity<JobStatusDto> compressSplat(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) SplatFormat targetFormat) {
        SplatFormat target = resolveCompressionTarget(targetFormat);
        return accepted(
                ModelSourceUtils.isUpload(file, url)
                        ? processingJobUseCase.submitWithUpload(ModelProcessingJobType.SPLAT_CONVERT, file, target)
                        : processingJobUseCase.submit(ModelProcessingJobType.SPLAT_CONVERT, url, target));
    }

    // ── Capabilities ──────────────────────────────────────────────────────────
    @Operation(summary = "List supported conversion target formats for splats and meshes.")
    @GetMapping(value = "/tools/convert/formats", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> supportedFormats() {
        return Map.of(
                "splat", Stream.of(SplatFormat.values()).map(Enum::name).toList(),
                "mesh", Stream.of(MeshExportFormat.values()).map(Enum::name).toList());
    }

    /**
     * Resolves the splat-compression target: defaults to {@link #DEFAULT_SPLAT_COMPRESSION_FORMAT} when the
     * caller omits one, and rejects a non-compressing (raw) target with a 400 — compression must yield a
     * compact container. Use {@code /tools/convert/splat} to target PLY/SPLAT.
     */
    private SplatFormat resolveCompressionTarget(SplatFormat requested) {
        SplatFormat target = requested == null ? DEFAULT_SPLAT_COMPRESSION_FORMAT : requested;
        if (!target.isCompressing()) {
            throw new KnownRuntimeError("Splat compression requires a compact target format (SPZ, SOG, KSPLAT,"
                    + " COMPRESSED_PLY); " + target + " is uncompressed. Use /tools/convert/splat for that target.");
        }
        return target;
    }
}

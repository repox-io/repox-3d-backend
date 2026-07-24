package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.toolkit.application.ports.in.SceneAndModelInfoUseCase;
import io.repox.api3d.app.usecases.toolkit.application.ports.out.ModelInfoAnalyzer;
import io.repox.api3d.app.usecases.toolkit.domain.ModelDetectionDto;
import io.repox.api3d.app.usecases.toolkit.domain.ModelTechnicalDetailsDto;
import io.repox.api3d.app.utils.UrlSourceUtils;
import io.repox.api3d.app.utils.UrlUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class SceneAndModelToolkitService implements SceneAndModelInfoUseCase {

    /** Bytes read from the start of an input for type detection — ample for any PLY header or magic number. */
    private static final int DETECTION_HEADER_BYTES = 64 * 1024;

    private final FileStoragePort fileStorage;
    private final UrlSourceUtils urlSourceUtils;
    private final ModelTypeDetector modelTypeDetector;
    private final Map<String, ModelInfoAnalyzer> analyzersByExtension;

    public SceneAndModelToolkitService(
            FileStoragePort fileStorage,
            UrlSourceUtils urlSourceUtils,
            ModelTypeDetector modelTypeDetector,
            List<ModelInfoAnalyzer> analyzers) {
        this.fileStorage = fileStorage;
        this.urlSourceUtils = urlSourceUtils;
        this.modelTypeDetector = modelTypeDetector;
        this.analyzersByExtension = analyzers.stream()
                .flatMap(analyzer -> analyzer.supportedExtensions().stream()
                        .map(ext -> Map.entry(ext.toLowerCase(Locale.ROOT), analyzer)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ModelTechnicalDetailsDto getModelTechnicalDetailsFromFile(File file) throws IOException {
        try {
            String ext = StringUtils.getFilenameExtension(file.getName());
            ModelInfoAnalyzer analyzer = ext == null ? null : analyzersByExtension.get(ext.toLowerCase(Locale.ROOT));
            if (analyzer == null) {
                throw new UnsupportedOperationException("Unsupported file.");
            }
            return analyzer.analyze(file);
        } catch (UnsupportedOperationException | IOException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new KnownRuntimeError("Failed to extract technical details for: " + file.getName(), e);
        } finally {
            fileStorage.delete(file);
        }
    }

    @Override
    public ModelTechnicalDetailsDto getModelTechnicalDetails(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return getModelTechnicalDetailsFromFile(fileStorage.save(inputStream, file.getOriginalFilename()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        }
    }

    @Override
    public ModelTechnicalDetailsDto getModelTechnicalDetailsFromUri(String url) {
        try {
            log.debug("Extracting technical details for file url : {}", url);
            return getModelTechnicalDetailsFromFile(fileStorage.saveFromUri(url, true));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<String> supportedFileExtensions() {
        return analyzersByExtension.keySet().stream().sorted().toList();
    }

    @Override
    public ModelDetectionDto detect(MultipartFile file) {
        String filename = file.getOriginalFilename();
        // The header supplier is invoked only when the extension cannot settle the type, so an unambiguous
        // upload is classified without ever reading the file body.
        return modelTypeDetector.detect(filename, () -> readHeaderBytes(file));
    }

    @Override
    public ModelDetectionDto detectFromUri(String url) {
        String filename = resolveDetectionFilename(url);
        // For a URL with an unambiguous extension no body is fetched at all; otherwise only the leading bytes
        // are read via a ranged request, never the whole model.
        return modelTypeDetector.detect(filename, () -> urlSourceUtils.fetchHeadBytes(url, DETECTION_HEADER_BYTES));
    }

    /**
     * Picks the filename whose extension drives detection. Prefers the URL path when it already carries an
     * extension (no network call); when it does not (e.g. an aggregator {@code /content} link), a single HEAD
     * recovers the {@code Content-Disposition} filename so magicless formats (.ksplat/.splat) are still
     * classified without downloading the body. Falls back to the (extensionless) path name when the server
     * advertises nothing, leaving detection to the header sniff.
     */
    private String resolveDetectionFilename(String url) {
        String pathFilename = StringUtils.getFilename(UrlUtils.toUri(url).getPath());
        if (StringUtils.getFilenameExtension(pathFilename) != null) {
            return pathFilename;
        }
        return urlSourceUtils
                .remoteFilename(url)
                .filter(name -> StringUtils.getFilenameExtension(name) != null)
                .orElse(pathFilename);
    }

    private byte[] readHeaderBytes(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(DETECTION_HEADER_BYTES);
        }
    }
}

package io.repox.api3d.app.usecases.storage.application;

import io.repox.api3d.app.usecases.storage.application.ports.out.FileStoragePort;
import io.repox.api3d.app.usecases.storage.domain.FileStorageSettings;
import io.repox.api3d.app.utils.UrlSourceUtils;
import io.repox.api3d.app.utils.UrlUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TempFileStorageService implements FileStoragePort {
    private final FileStorageSettings fileStorageProperties;

    private static final Pattern CONTENT_DISPOSITION_FILENAME =
            Pattern.compile("filename\\*?=['\"]?(?:UTF-8'')?([^;\"'\\s]+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    @Override
    public File save(InputStream inputStream, String originalFilename) throws IOException {
        return save(inputStream, originalFilename, false);
    }

    @Override
    public File save(InputStream inputStream, String originalFilename, boolean addRandomFilenamePrefix)
            throws IOException {
        Path filePath = inputDir().resolve(inputFilename(originalFilename, addRandomFilenamePrefix));
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toFile();
    }

    /**
     * Builds the on-disk name for a stored input file: an optional random UUID segment for collision-safety
     * between concurrent requests, then the original filename. No shared marker prefix is needed — the whole
     * {@link #getInputDirectory() input directory} is wiped wholesale on startup.
     */
    private String inputFilename(String originalFilename, boolean addRandomFilenamePrefix) {
        String uniqueSegment = addRandomFilenamePrefix ? UUID.randomUUID() + "_" : "";
        return uniqueSegment + originalFilename;
    }

    /** The {@code input} sub-directory, created on demand. */
    private Path inputDir() {
        return ensureDirectory(Paths.get(fileStorageProperties.storageDir(), "input"));
    }

    /** The {@code output} sub-directory, created on demand. */
    private Path outputDir() {
        return ensureDirectory(Paths.get(fileStorageProperties.storageDir(), "output"));
    }

    private Path ensureDirectory(Path dir) {
        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create storage directory: " + dir, e);
        }
    }

    @Override
    public File saveFromUri(String uri, String originalFilename) throws IOException {
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(buildGetRequest(uri), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Server returned HTTP response code: " + response.statusCode() + " for URL: " + uri);
            }
            String effectiveFilename = resolveEffectiveFilename(
                    originalFilename,
                    response.headers().firstValue("Content-Disposition").orElse(null));
            Path filePath = inputDir().resolve(inputFilename(effectiveFilename, false));
            try (InputStream in = response.body()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return filePath.toFile();
        } catch (InterruptedException e) {
            throw new IOException("Request interrupted for URL: " + uri, e);
        }
    }

    @Override
    public File saveFromUri(String uri) throws IOException {
        return saveFromUri(uri, false);
    }

    @Override
    public File saveFromUri(String uri, boolean addRandomFilenamePrefix) throws IOException {
        try {
            HttpResponse<InputStream> response =
                    httpClient.send(buildGetRequest(uri), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Server returned HTTP response code: " + response.statusCode() + " for URL: " + uri);
            }
            String filename = inputFilename(resolveFilenameFromResponse(uri, response), addRandomFilenamePrefix);
            Path filePath = inputDir().resolve(filename);
            try (InputStream in = response.body()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return filePath.toFile();
        } catch (InterruptedException e) {
            throw new IOException("Request interrupted for URL: " + uri, e);
        }
    }

    private HttpRequest buildGetRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .GET()
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .build();
    }

    private String resolveFilenameFromResponse(String uri, HttpResponse<InputStream> response) {
        return response.headers()
                .firstValue("Content-Disposition")
                .map(UrlSourceUtils::filenameFromContentDisposition)
                .filter(StringUtils::hasText)
                .orElseGet(() -> StringUtils.getFilename(UrlUtils.toUri(uri).getPath()));
    }

    private String resolveEffectiveFilename(String originalFilename, String contentDisposition) {
        if (StringUtils.getFilenameExtension(originalFilename) != null) {
            return originalFilename;
        }
        if (contentDisposition == null) {
            return originalFilename;
        }
        Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(contentDisposition);
        if (!matcher.find()) {
            return originalFilename;
        }
        String ext = StringUtils.getFilenameExtension(matcher.group(1));
        return ext != null ? originalFilename + "." + ext : originalFilename;
    }

    @Override
    public Optional<File> get(String fileId) {
        return Optional.empty();
    }

    @Override
    public boolean delete(File file) {
        return file.delete();
    }

    /**
     * Empties the input directory: deletes every regular file directly under {@link #getInputDirectory()}.
     * Intended to run once at startup, when this process holds no in-flight requests, so every file present
     * is an orphan from a previous (possibly crashed) run. The output directory is left untouched — its
     * lifecycle is governed by retention-based sweeping.
     */
    @Override
    public void cleanupInputDirectory() {
        File dir = inputDir().toFile();
        File[] files = dir.listFiles();
        if (files == null) {
            log.warn("Input directory not accessible for cleanup: {}", dir.getAbsolutePath());
            return;
        }
        int deleted = 0;
        for (File file : files) {
            if (file.isFile()) {
                if (file.delete()) {
                    deleted++;
                } else {
                    log.warn("Could not delete leftover input file: {}", file.getAbsolutePath());
                }
            }
        }
        log.info("Input cleanup removed {} leftover file(s) from {}", deleted, dir.getAbsolutePath());
    }

    @Override
    public File getStorageDirectory() {
        return new File(fileStorageProperties.storageDir());
    }

    @Override
    public File getInputDirectory() {
        return inputDir().toFile();
    }

    @Override
    public File getOutputDirectory() {
        return outputDir().toFile();
    }
}

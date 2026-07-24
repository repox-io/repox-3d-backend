package io.repox.api3d.app.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads content and metadata from remote HTTP(S) sources without persisting anything. These concerns are
 * about fetching from a URL, not about temporary-file storage, so they live here rather than on the file
 * storage port: a cheap existence probe, a ranged header read for lightweight inspection, and a filename
 * lookup from {@code Content-Disposition}.
 */
@Slf4j
@Component
public class UrlSourceUtils {

    /** Upper bound on a HEAD probe, so a slow host cannot stall the request thread. */
    private static final int HEAD_CHECK_TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    /**
     * Cheaply checks whether the resource at the given URI exists, via an HTTP {@code HEAD} request (no body
     * is downloaded). Intended as a submit-time guard for URL sources, so an obviously bad link is rejected
     * before a job is enqueued while the actual download stays deferred to the worker.
     *
     * @param uri the URI to probe
     * @return {@code true} for a successful response (or when the server does not support {@code HEAD}, which
     *     cannot disprove existence); {@code false} when the resource is missing/forbidden or the host is
     *     unreachable
     */
    public boolean remoteResourceExists(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(HEAD_CHECK_TIMEOUT_SECONDS))
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .build();
        try {
            int status = httpClient
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            // 2xx/3xx: present. 405/501: the server refuses HEAD but that cannot disprove the resource, so
            // don't block — the worker's GET will settle it. Everything else (404/403/410, 5xx): reject.
            return status < 400 || status == 405 || status == 501;
        } catch (IOException e) {
            log.warn("HEAD existence check failed for URL {}: {}", uri, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("HEAD existence check interrupted for URL {}", uri);
            return false;
        }
    }

    /**
     * Fetches at most {@code maxBytes} from the start of the resource at the given URI, without persisting
     * anything. Issues an HTTP {@code Range} request so a compliant server sends only the requested prefix
     * ({@code 206}); a server that ignores {@code Range} returns the full body ({@code 200}) which is read
     * and truncated to {@code maxBytes} then abandoned. Intended for cheap, header-only inspection (e.g.
     * type detection) of large remote models.
     *
     * @param uri the URI to read from
     * @param maxBytes the maximum number of leading bytes to return
     * @return the leading bytes (may be shorter than {@code maxBytes} if the resource is smaller)
     * @throws IOException if the request fails or the server returns a non-success status
     */
    public byte[] fetchHeadBytes(String uri, int maxBytes) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .GET()
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .header("Range", "bytes=0-" + (maxBytes - 1))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // 206 (Range honoured) and 200 (full body — server ignored Range) are both fine; we read at most
            // maxBytes then drop the connection, so a full-body response is truncated rather than downloaded.
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Server returned HTTP response code: " + response.statusCode() + " for URL: " + uri);
            }
            try (InputStream in = response.body()) {
                return in.readNBytes(maxBytes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted for URL: " + uri, e);
        }
    }

    /**
     * Resolves a filename for the resource from an HTTP {@code HEAD} request's {@code Content-Disposition}
     * header (no body downloaded), or {@link Optional#empty()} when the server sends none or is unreachable.
     * Intended to recover a file extension for a URL whose path carries none (e.g. an aggregator
     * {@code /content} link), so a magicless format can still be classified without fetching the body.
     *
     * @param uri the URI to probe
     * @return the server-advertised filename if present, otherwise empty
     */
    public Optional<String> remoteFilename(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(HEAD_CHECK_TIMEOUT_SECONDS))
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                return Optional.empty();
            }
            return response.headers()
                    .firstValue("Content-Disposition")
                    .map(UrlSourceUtils::filenameFromContentDisposition)
                    .filter(StringUtils::hasText);
        } catch (IOException e) {
            log.warn("HEAD filename lookup failed for URL {}: {}", uri, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("HEAD filename lookup interrupted for URL {}", uri);
            return Optional.empty();
        }
    }

    /**
     * Extracts the bare filename from a {@code Content-Disposition} header value, or {@code null} if it is
     * absent or unparseable. Shared with the file-storage download path, which resolves the saved filename
     * from the same header.
     */
    public static String filenameFromContentDisposition(String contentDisposition) {
        try {
            String filename = ContentDisposition.parse(contentDisposition).getFilename();
            return StringUtils.hasText(filename) ? StringUtils.getFilename(filename) : null;
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse Content-Disposition header '{}': {}", contentDisposition, e.getMessage());
            return null;
        }
    }
}

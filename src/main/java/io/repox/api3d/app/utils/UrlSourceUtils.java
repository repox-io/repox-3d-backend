package io.repox.api3d.app.utils;

import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads content and metadata from remote HTTP(S) sources without persisting anything. These concerns are
 * about fetching from a URL, not about temporary-file storage, so they live here rather than on the file
 * storage port: a cheap existence probe, a ranged header read for lightweight inspection, and metadata
 * (size, filename, content type) recovered from response headers.
 */
@Slf4j
@Component
public class UrlSourceUtils {

    /** Upper bound on a HEAD probe, so a slow host cannot stall the request thread. */
    private static final int HEAD_CHECK_TIMEOUT_SECONDS = 10;

    private final HttpClient httpClient =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    /**
     * What a single {@code HEAD} reveals about a remote source. Any field is null when the server does not
     * advertise it; in particular a null {@code contentLength} means "unknown", not empty.
     *
     * @param contentLength the advertised body size in bytes
     * @param filename the filename from {@code Content-Disposition}
     * @param contentType the media type, without parameters
     */
    public record RemoteSourceInfo(Long contentLength, String filename, String contentType) {}

    /**
     * Cheaply confirms that the resource at the given URI exists, via an HTTP {@code HEAD} request (no body is
     * downloaded). Intended as a submit-time guard for URL sources, so a bad link is rejected before a job is
     * enqueued while the actual download stays deferred to the worker.
     *
     * @param uri the URI to probe
     * @throws KnownRuntimeError when the resource is missing/forbidden (reporting the host's own status) or
     *     the host is unreachable. Returns normally for a successful response, and also when the server does
     *     not support {@code HEAD}, which cannot disprove the resource.
     */
    public void verifyRemoteResourceExists(String uri) {
        try {
            int status = httpClient
                    .send(headRequest(uri), HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            // 2xx/3xx: present. 405/501: the server refuses HEAD but that cannot disprove the resource, so
            // don't block — the worker's GET will settle it. Everything else (404/403/410, 5xx): reject.
            if (status >= 400 && !headUnsupported(status)) {
                throw upstreamStatusError(uri, status);
            }
        } catch (IOException e) {
            throw unreachableError(uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unreachableError(uri, e);
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
     * @throws KnownRuntimeError if the server returns a non-success status (reporting that status) or the host
     *     is unreachable
     */
    public byte[] fetchHeadBytes(String uri, int maxBytes) {
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
                throw upstreamStatusError(uri, response.statusCode());
            }
            try (InputStream in = response.body()) {
                return in.readNBytes(maxBytes);
            }
        } catch (IOException e) {
            throw unreachableError(uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unreachableError(uri, e);
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
        return remoteSourceInfo(uri).map(RemoteSourceInfo::filename).filter(StringUtils::hasText);
    }

    /**
     * Reads everything a single HTTP {@code HEAD} reveals about a remote source — its size, advertised
     * filename and content type — without downloading any of the body. One request serves all three because
     * the response that carries {@code Content-Length} carries {@code Content-Disposition} and
     * {@code Content-Type} alongside it.
     *
     * @param uri the URI to probe
     * @return the advertised metadata; individual fields are null when the server advertises them not. Empty
     *     means only that the server does not support {@code HEAD} — the caller should fall back rather than
     *     treat the resource as absent.
     * @throws KnownRuntimeError when the resource is missing/forbidden (reporting the host's own status) or
     *     the host is unreachable
     */
    public Optional<RemoteSourceInfo> remoteSourceInfo(String uri) {
        try {
            HttpResponse<Void> response = httpClient.send(headRequest(uri), HttpResponse.BodyHandlers.discarding());
            if (headUnsupported(response.statusCode())) {
                // The server refuses HEAD, which says nothing about the resource: report "no metadata" and let
                // the caller settle it with a GET, rather than failing a resource that is probably fine.
                return Optional.empty();
            }
            if (response.statusCode() >= 400) {
                throw upstreamStatusError(uri, response.statusCode());
            }
            Long contentLength = response.headers().firstValueAsLong("Content-Length").stream()
                    .boxed()
                    .findFirst()
                    .orElse(null);
            String filename = response.headers()
                    .firstValue("Content-Disposition")
                    .map(UrlSourceUtils::filenameFromContentDisposition)
                    .filter(StringUtils::hasText)
                    .orElse(null);
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .map(UrlSourceUtils::bareContentType)
                    .filter(StringUtils::hasText)
                    .orElse(null);
            return Optional.of(new RemoteSourceInfo(contentLength, filename, contentType));
        } catch (IOException e) {
            throw unreachableError(uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unreachableError(uri, e);
        }
    }

    /**
     * Recovers the total size of a remote resource whose {@code HEAD} advertised no {@code Content-Length}
     * (a chunked response, or a server that answers {@code HEAD} sparsely), by asking for a single byte and
     * reading the total out of the {@code Content-Range} header. Nothing is downloaded: the body is closed
     * without being read, so even a server that ignores {@code Range} and starts sending the whole file
     * costs only the connection.
     *
     * @param uri the URI to probe
     * @return the total size in bytes. Empty means the probe succeeded but the server states no total (a
     *     {@code bytes 0-0/*} range, or a {@code 200} without {@code Content-Length}) — the size is genuinely
     *     unknown, as opposed to unobtainable.
     * @throws KnownRuntimeError if the server answers with an unexpected status (reporting that status) or the
     *     host is unreachable
     */
    public OptionalLong remoteContentLengthByRange(String uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .GET()
                .timeout(Duration.ofSeconds(HEAD_CHECK_TIMEOUT_SECONDS))
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .header("Range", "bytes=0-0")
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream ignored = response.body()) {
                // 206: the range was honoured, and Content-Range carries the total. 200: the server ignored
                // Range and is offering the whole body, whose Content-Length is that same total — we close
                // without reading either way, so no payload is transferred.
                if (response.statusCode() == 206) {
                    return response.headers()
                            .firstValue("Content-Range")
                            .map(UrlSourceUtils::totalFromContentRange)
                            .orElse(OptionalLong.empty());
                }
                if (response.statusCode() == 200) {
                    return response.headers().firstValueAsLong("Content-Length");
                }
                throw upstreamStatusError(uri, response.statusCode());
            }
        } catch (IOException e) {
            throw unreachableError(uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unreachableError(uri, e);
        }
    }

    /**
     * Reports a failure the remote host declared. A {@code 4xx} is relayed verbatim — it describes the link the
     * caller gave us, so the caller should see it — while a {@code 5xx} becomes a {@code 502}: their outage is
     * this service acting as a failing gateway, not this service being unhealthy, and conflating the two would
     * make our own monitoring read an upstream outage as ours.
     */
    public static KnownRuntimeError upstreamStatusError(String uri, int status) {
        int reported = status >= 500 ? HttpStatus.BAD_GATEWAY.value() : status;
        return KnownRuntimeError.withStatus(reported, "Source URL returned HTTP " + status + ": " + uri);
    }

    /**
     * Reports a failure to reach the host at all (connection refused, DNS, timeout, interrupt) as a 400: no
     * status was ever received, and an unusable link is a problem with the request. The cause's message is
     * folded into the text because only {@code getMessage()} survives into a job's recorded error.
     */
    private static KnownRuntimeError unreachableError(String uri, Exception cause) {
        String reason = StringUtils.hasText(cause.getMessage())
                ? cause.getMessage()
                : cause.getClass().getSimpleName();
        return KnownRuntimeError.withStatus(
                HttpStatus.BAD_REQUEST.value(), "Could not reach source URL " + uri + ": " + reason, cause);
    }

    /** {@code 405}/{@code 501}: the server refuses {@code HEAD}, which cannot disprove the resource. */
    private static boolean headUnsupported(int status) {
        return status == 405 || status == 501;
    }

    /** The shared {@code HEAD} probe: no body, bounded by {@link #HEAD_CHECK_TIMEOUT_SECONDS}. */
    private HttpRequest headRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(UrlUtils.toUri(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(HEAD_CHECK_TIMEOUT_SECONDS))
                .header("User-Agent", "curl/8.6.0")
                .header("Accept", "*/*")
                .build();
    }

    /**
     * Extracts the total size from a {@code Content-Range} header value ({@code bytes 0-0/12345}), or empty
     * when the server sent an unknown total ({@code bytes 0-0/*}) or an unparseable value.
     */
    private static OptionalLong totalFromContentRange(String contentRange) {
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0) {
            return OptionalLong.empty();
        }
        String total = contentRange.substring(slash + 1).trim();
        try {
            return OptionalLong.of(Long.parseLong(total));
        } catch (NumberFormatException e) {
            // "*" is the legitimate spelling of "I don't know the total", so this is not worth a warning.
            log.debug("Could not read a total size from Content-Range '{}'", contentRange);
            return OptionalLong.empty();
        }
    }

    /** Strips any parameters (e.g. {@code ; charset=utf-8}) from a {@code Content-Type} header value. */
    private static String bareContentType(String contentType) {
        int semicolon = contentType.indexOf(';');
        return (semicolon < 0 ? contentType : contentType.substring(0, semicolon)).trim();
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

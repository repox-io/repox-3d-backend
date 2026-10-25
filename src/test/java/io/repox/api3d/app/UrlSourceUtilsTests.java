package io.repox.api3d.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.repox.api3d.app.utils.UrlSourceUtils;
import io.repox.api3d.app.utils.exception.KnownRuntimeError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link UrlSourceUtils} against a real HTTP server, because every behaviour under test is status
 * and header handling that a mock would define away. The JDK's own {@link HttpServer} is used so no HTTP
 * stubbing dependency is needed.
 *
 * <p>The distinction being pinned throughout: a failure to <em>access</em> the URL throws and reports the
 * host's status, while a successful probe of a host that simply advertises nothing returns empty.
 */
class UrlSourceUtilsTests {

    private final UrlSourceUtils urlSourceUtils = new UrlSourceUtils();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    // ---------------------------------------------------------------- failures are surfaced

    @Test
    void missingResourceReportsTheHostsOwnStatus() {
        String url = route("/missing", exchange -> respond(exchange, 404, null));

        assertThatThrownBy(() -> urlSourceUtils.remoteSourceInfo(url))
                .isInstanceOf(KnownRuntimeError.class)
                .hasMessageContaining("404")
                .hasMessageContaining(url)
                .extracting(e -> ((KnownRuntimeError) e).httpStatus())
                .isEqualTo(404);
    }

    @Test
    void forbiddenResourceIsRelayedVerbatim() {
        String url = route("/secret", exchange -> respond(exchange, 403, null));

        assertThatThrownBy(() -> urlSourceUtils.remoteSourceInfo(url))
                .isInstanceOf(KnownRuntimeError.class)
                .extracting(e -> ((KnownRuntimeError) e).httpStatus())
                .isEqualTo(403);
    }

    @Test
    void upstreamServerErrorBecomesBadGatewayRatherThanOurOwnOutage() {
        String url = route("/down", exchange -> respond(exchange, 503, null));

        assertThatThrownBy(() -> urlSourceUtils.remoteSourceInfo(url))
                .isInstanceOf(KnownRuntimeError.class)
                // Their 503 must not be echoed as ours, or our uptime monitoring reads an upstream outage
                // as this service being unhealthy. The message still names the real status.
                .hasMessageContaining("503")
                .extracting(e -> ((KnownRuntimeError) e).httpStatus())
                .isEqualTo(502);
    }

    @Test
    void unreachableHostIsABadRequestNamingTheUrl() {
        // Nothing is listening on this port: the connection fails before any status exists.
        String url = "http://127.0.0.1:1/never-there.glb";

        assertThatThrownBy(() -> urlSourceUtils.remoteSourceInfo(url))
                .isInstanceOf(KnownRuntimeError.class)
                .hasMessageContaining(url)
                .extracting(e -> ((KnownRuntimeError) e).httpStatus())
                .isEqualTo(400);
    }

    @Test
    void rangedProbeFailureIsSurfacedRatherThanReportedAsAnUnknownSize() {
        String url = route("/broken", exchange -> respond(exchange, 500, null));

        assertThatThrownBy(() -> urlSourceUtils.remoteContentLengthByRange(url))
                .isInstanceOf(KnownRuntimeError.class)
                .extracting(e -> ((KnownRuntimeError) e).httpStatus())
                .isEqualTo(502);
    }

    // ---------------------------------------------------------------- "unknown" still means unknown

    @Test
    void serverRefusingHeadYieldsNoMetadataButIsNotAFailure() {
        // 405 says the server will not answer HEAD; it says nothing about the resource, so callers must be
        // free to fall back to a GET instead of the request being failed outright.
        String url = route("/nohead", exchange -> respond(exchange, 405, null));

        assertThat(urlSourceUtils.remoteSourceInfo(url)).isEmpty();
        assertThat(urlSourceUtils.remoteFilename(url)).isEmpty();
    }

    @Test
    void headRefusalDoesNotFailTheExistenceGuard() {
        String url = route("/nohead", exchange -> respond(exchange, 405, null));

        urlSourceUtils.verifyRemoteResourceExists(url); // must not throw
    }

    @Test
    void reachableResourcePassesTheExistenceGuard() {
        String url = route("/ok", exchange -> respond(exchange, 200, null));

        urlSourceUtils.verifyRemoteResourceExists(url); // must not throw
    }

    @Test
    void unknownRangeTotalIsReportedAsAnUnknownSize() {
        String url = route("/star", exchange -> {
            exchange.getResponseHeaders().add("Content-Range", "bytes 0-0/*");
            respond(exchange, 206, "x");
        });

        assertThat(urlSourceUtils.remoteContentLengthByRange(url)).isEmpty();
    }

    @Test
    void rangeIgnoringServerWithoutAContentLengthIsReportedAsAnUnknownSize() {
        // Chunked: a 200 that states no length. The probe succeeded, so this is unknown, not a failure.
        String url = route("/chunked", exchange -> respondChunked(exchange, "some bytes"));

        assertThat(urlSourceUtils.remoteContentLengthByRange(url)).isEmpty();
    }

    // ---------------------------------------------------------------- the happy paths still work

    @Test
    void metadataIsReadFromASingleHead() {
        String url = route("/model", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "model/gltf-binary; charset=utf-8");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"scene.glb\"");
            exchange.getResponseHeaders().add("Content-Length", "4242");
            respond(exchange, 200, null);
        });

        Optional<UrlSourceUtils.RemoteSourceInfo> info = urlSourceUtils.remoteSourceInfo(url);

        assertThat(info).isPresent();
        assertThat(info.get().contentLength()).isEqualTo(4242L);
        assertThat(info.get().filename()).isEqualTo("scene.glb");
        assertThat(info.get().contentType()).isEqualTo("model/gltf-binary"); // parameters stripped
    }

    @Test
    void totalSizeIsRecoveredFromContentRange() {
        String url = route("/ranged", exchange -> {
            exchange.getResponseHeaders().add("Content-Range", "bytes 0-0/9876");
            respond(exchange, 206, "x");
        });

        assertThat(urlSourceUtils.remoteContentLengthByRange(url)).hasValue(9876L);
    }

    @Test
    void headerBytesAreFetchedWithoutTheWholeBody() {
        String url = route("/head-bytes", exchange -> respond(exchange, 200, "NGSP-and-then-some-more"));

        assertThat(urlSourceUtils.fetchHeadBytes(url, 4)).containsExactly('N', 'G', 'S', 'P');
    }

    // ---------------------------------------------------------------- helpers

    /** Registers a handler and returns the absolute URL that reaches it. */
    private String route(String path, ThrowingHandler handler) {
        server.createContext(path, exchange -> {
            try (exchange) {
                handler.handle(exchange);
            }
        });
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /**
     * Writes a response. A HEAD request never carries a body, so the length is declared as -1 there — the
     * headers under test still reach the client.
     */
    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        boolean head = "HEAD".equals(exchange.getRequestMethod());
        byte[] bytes = body == null ? new byte[0] : body.getBytes();
        exchange.sendResponseHeaders(status, head || bytes.length == 0 ? -1 : bytes.length);
        if (!head && bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
    }

    /**
     * Writes a chunked response — a body length of 0 tells {@link HttpServer} to use chunked encoding, so no
     * {@code Content-Length} is sent. This is the "reached the host, it states no size" case.
     */
    private static void respondChunked(HttpExchange exchange, String body) throws IOException {
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().write(body.getBytes());
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}

package io.repox.api3d.app.utils;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

public class UrlUtils {

    private UrlUtils() {}

    /**
     * Parses a URL string into a valid {@link URI}, handling both already-encoded and raw input.
     *
     * <p>An already percent-encoded, browser-ready URL (e.g. {@code
     * https://host/1Kr3267-75,%20obj3.glb/content}) is passed through untouched — its existing
     * escapes are preserved rather than re-encoded, which would otherwise turn {@code %20} into
     * {@code %2520} and cause the origin to 404. A genuinely raw URL with unencoded illegal
     * characters (e.g. a literal space in {@code https://host/head_of_figurine (1).glb/content})
     * is percent-encoded instead.
     *
     * @param url the URL string, already encoded or raw
     * @return a valid URI
     */
    public static URI toUri(String url) {
        try {
            // Normal case: treat the input as already encoded and keep its escapes verbatim.
            return UriComponentsBuilder.fromUriString(url).build(true).toUri();
        } catch (IllegalArgumentException e) {
            // The input contained an unencoded illegal character (e.g. a literal space), so it
            // was raw after all — encode it.
            return UriComponentsBuilder.fromUriString(url).build().encode().toUri();
        }
    }
}

package io.repox.api3d.app.usecases.toolkit.domain;

/**
 * Result of a {@code /tools/info} probe: what the source <em>is</em>, as delivered, rather than what the
 * model inside it contains (that is {@code /tools/details}). Answered without ever downloading the body —
 * from the upload itself, or from a single {@code HEAD} against the URL.
 *
 * <p>Every field may be null when the source does not advertise it. A null {@code sizeBytes} means the size
 * is <em>unknown</em> (e.g. a chunked response with no {@code Content-Length}), not that the source is empty.
 *
 * @param sizeBytes the source size in bytes, or null when unknown
 * @param filename the filename, from the upload or the URL, or null when none can be resolved
 * @param contentType the media type, or null when unadvertised
 */
public record ModelSourceInfoDto(Long sizeBytes, String filename, String contentType) {}

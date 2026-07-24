package io.repox.api3d.app.usecases.toolkit.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Parses a PLY file's ASCII header (always text until {@code end_header}, regardless of the body
 * encoding) into its vertex/face counts and a gaussian-splat flag.
 *
 * <p>A single parser shared by the detail analyzer (full-file analysis) and the {@code /tools/detect}
 * sniffer (header bytes only). PLY files produced by 3D Gaussian Splatting carry per-vertex gaussian
 * attributes ({@code f_dc_0}, {@code scale_0}, {@code rot_0}, {@code opacity}); when that signature is
 * present each vertex is a gaussian.
 */
public record PlyHeader(Long vertexCount, Long faceCount, boolean gaussianSplat) {

    private static final Set<String> GAUSSIAN_PROPERTY_SIGNATURE = Set.of("f_dc_0", "scale_0", "rot_0", "opacity");

    /**
     * Parses the PLY header from the start of the given stream. Returns {@link Optional#empty()} when the
     * stream does not begin with the {@code ply} magic (i.e. it is not a PLY at all) so a caller sniffing an
     * unknown input can fall through to other format checks. The stream is read only up to {@code end_header}
     * (or EOF); {@code vertexCount}/{@code faceCount} are {@code null} when the header omits that element.
     */
    public static Optional<PlyHeader> parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
        String firstLine = reader.readLine();
        if (firstLine == null || !firstLine.trim().equalsIgnoreCase("ply")) {
            return Optional.empty();
        }

        Long vertexCount = null;
        Long faceCount = null;
        boolean gaussianSplat = false;
        String currentElement = null;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase("end_header")) {
                break;
            }
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length >= 3 && tokens[0].equalsIgnoreCase("element")) {
                currentElement = tokens[1].toLowerCase();
                long count = Long.parseLong(tokens[2]);
                if ("vertex".equals(currentElement)) {
                    vertexCount = count;
                } else if ("face".equals(currentElement)) {
                    faceCount = count;
                }
            } else if (tokens.length >= 3
                    && tokens[0].equalsIgnoreCase("property")
                    && "vertex".equals(currentElement)
                    && GAUSSIAN_PROPERTY_SIGNATURE.contains(tokens[tokens.length - 1].toLowerCase())) {
                gaussianSplat = true;
            }
        }

        return Optional.of(new PlyHeader(vertexCount, faceCount, gaussianSplat));
    }
}

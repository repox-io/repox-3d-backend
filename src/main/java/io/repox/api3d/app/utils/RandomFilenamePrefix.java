package io.repox.api3d.app.utils;

import java.util.regex.Pattern;

/**
 * The {@code <uuid>_} collision-avoidance prefix added to stored input files (and inherited by produced
 * output filenames). Stripped from client-facing download names so the on-disk file keeps its unique name
 * while the download stays clean.
 */
public final class RandomFilenamePrefix {

    private static final Pattern PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_");

    private RandomFilenamePrefix() {}

    /** Removes a leading {@code <uuid>_} segment if present; otherwise returns the name unchanged. */
    public static String strip(String filename) {
        return filename == null ? null : PATTERN.matcher(filename).replaceFirst("");
    }
}

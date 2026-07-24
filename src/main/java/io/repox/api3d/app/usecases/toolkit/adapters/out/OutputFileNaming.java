package io.repox.api3d.app.usecases.toolkit.adapters.out;

import io.repox.api3d.app.utils.TempFileNaming;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared naming helpers for converter/compressor output files. */
final class OutputFileNaming {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private OutputFileNaming() {}

    /**
     * Derives the base name for an output file from a stored input's filename: strips the
     * {@link TempFileNaming#TEMP_PREFIX} marker (so produced outputs never carry it) and all extensions
     * (e.g. {@code model.compressed.ply} -> {@code model}) so names do not accumulate stacked extensions.
     * Any per-file random UUID segment is preserved to keep concurrent outputs collision-safe; it is
     * stripped from the client-facing download name separately.
     */
    static String baseName(String filename) {
        String name = TempFileNaming.stripPrefix(filename);
        int dot = name.indexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Current local time formatted as {@code yyyyMMdd_HHmmss}, for suffixing output filenames so repeated
     * conversions of the same input do not overwrite one another and the produced order is legible.
     */
    static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP);
    }
}

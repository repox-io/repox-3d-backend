package io.repox.api3d.app.utils;

/**
 * Naming convention for the ephemeral scratch files the app writes to the temp storage directory.
 *
 * <p>Every uploaded or URL-fetched input is stored with the {@link #TEMP_PREFIX} marker so a startup
 * sweep can safely delete leftovers from a previous (possibly crashed) run — matching only files the app
 * itself created, without touching anything else that may share the directory. The marker is deliberately
 * kept off produced output files (they are stripped via {@link #stripPrefix}) so the client-facing
 * download names stay clean.
 */
public final class TempFileNaming {

    /** Leading marker identifying app-created temporary input files; used by the startup sweep. */
    public static final String TEMP_PREFIX = "repox3d-tmp-";

    private TempFileNaming() {}

    /** Removes the {@link #TEMP_PREFIX} marker from a filename if present; otherwise returns it unchanged. */
    public static String stripPrefix(String filename) {
        return filename != null && filename.startsWith(TEMP_PREFIX)
                ? filename.substring(TEMP_PREFIX.length())
                : filename;
    }
}

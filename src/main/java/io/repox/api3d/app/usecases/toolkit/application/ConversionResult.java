package io.repox.api3d.app.usecases.toolkit.application;

import io.repox.api3d.app.utils.RandomFilenamePrefix;
import java.io.File;

/**
 * The product of a conversion/compression: the output file plus a suggested download name and a cleanup
 * hook. Conversion happens eagerly in the service (so failures surface as proper HTTP errors before the
 * response body starts), and the resulting output file is then streamed to the client. Because a
 * {@code StreamingResponseBody} runs <em>after</em> the controller returns, the temp output file must be
 * deleted once streaming finishes — the controller calls {@link #close()} in the streaming callback's
 * {@code finally} (e.g. via try-with-resources) to trigger {@code onClose}.
 */
public final class ConversionResult implements AutoCloseable {

    private final File file;
    private final String downloadName;
    private final Runnable onClose;

    public ConversionResult(File file, String downloadName, Runnable onClose) {
        this.file = file;
        this.downloadName = downloadName;
        this.onClose = onClose;
    }

    /**
     * Builds a result for an output {@code file}, deriving the client-facing download name from the
     * file name with any {@code <uuid>_} collision-avoidance prefix stripped. The on-disk file is left
     * untouched (it keeps its unique name); {@code onClose} deletes it once streaming completes.
     */
    public static ConversionResult forOutput(File file, Runnable onClose) {
        return new ConversionResult(file, RandomFilenamePrefix.strip(file.getName()), onClose);
    }

    public File file() {
        return file;
    }

    public String downloadName() {
        return downloadName;
    }

    @Override
    public void close() {
        onClose.run();
    }
}

package io.repox.api3d.app.usecases.storage.application.ports.out;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Defines operations for temporary file storage.
 * Files stored here should be considered ephemeral and may be deleted
 * after a set period or once processed.
 */
public interface FileStoragePort {
    /**
     * Saves an InputStream as a temporary file.
     *
     * @param inputStream the input data
     * @param originalFilename the name of the original file (optional)
     * @return the unique identifier (or path) of the stored file
     * @throws IOException if saving fails
     */
    File save(InputStream inputStream, String originalFilename) throws IOException;

    /**
     * Saves an InputStream as a temporary file.
     *
     * @param inputStream the input data
     * @param originalFilename the name of the original file (optional)
     * @param addRandomFilenamePrefix when {@code true}, a random prefix is prepended to the filename to
     *     avoid collisions between concurrent requests for identically-named files
     * @return the stored file
     * @throws IOException if saving fails
     */
    File save(InputStream inputStream, String originalFilename, boolean addRandomFilenamePrefix) throws IOException;

    File saveFromUri(String uri, String originalFilename) throws IOException;

    /**
     * Downloads the resource at the given URI and saves it as a temporary file, deriving the filename
     * from the server response (Content-Disposition) and falling back to the URL path.
     *
     * <p>Equivalent to {@code saveFromUri(uri, false)}.
     *
     * @param uri the URI to download from
     * @return the stored file
     * @throws IOException if downloading or saving fails
     */
    File saveFromUri(String uri) throws IOException;

    /**
     * Downloads the resource at the given URI and saves it as a temporary file, deriving the filename
     * from the server response (Content-Disposition) and falling back to the URL path.
     *
     * @param uri the URI to download from
     * @param addRandomFilenamePrefix when {@code true}, a random prefix is prepended to the resolved
     *     filename to avoid collisions between concurrent requests for identically-named files
     * @return the stored file
     * @throws IOException if downloading or saving fails
     */
    File saveFromUri(String uri, boolean addRandomFilenamePrefix) throws IOException;

    /**
     * Retrieves a stored temporary file by its ID.
     *
     * @param fileId the identifier of the file
     * @return an Optional containing the File if it exists and is not expired
     */
    Optional<File> get(String fileId);

    /**
     * Deletes a specific temporary file.
     *
     * @return true if the file was deleted, false if it didn’t exist
     */
    boolean delete(File file);

    /**
     * Wipes the input directory wholesale. Every uploaded/URL-fetched file (and any short-lived
     * intermediate) lives under {@link #getInputDirectory()}; on startup — when no request is in flight —
     * the whole directory can be emptied to reclaim leftovers from a previous (possibly crashed) run.
     */
    void cleanupInputDirectory();

    /**
     * Returns the root directory for temporary files.
     *
     * @return the directory used to store temporary files
     */
    File getStorageDirectory();

    /**
     * Returns the {@code input} sub-directory where uploaded/downloaded source files and intermediates are
     * stored. Emptied wholesale on startup.
     */
    File getInputDirectory();

    /**
     * Returns the {@code output} sub-directory where produced deliverables (conversion/compression results)
     * are written. Managed by a retention-based sweep, never wiped on startup.
     */
    File getOutputDirectory();
}

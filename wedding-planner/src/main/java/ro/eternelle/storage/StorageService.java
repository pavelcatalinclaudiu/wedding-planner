package ro.eternelle.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Abstraction over file storage.
 * <p>
 * The local implementation writes to the filesystem.
 * Swap in a cloud implementation (S3, R2) without touching any resource class.
 * <p>
 * Stored paths are relative to the storage root and include the folder, e.g.
 * {@code "vendors/3f2a1b.jpg"}.  Pass this value to {@link #delete} or
 * {@link #resolve} as-is.
 */
public interface StorageService {

    /**
     * Persist {@code data} under {@code folder} and return the relative stored
     * path, e.g. {@code "vendors/3f2a1b.jpg"}.
     *
     * @param data             the raw bytes to store
     * @param originalFilename original client filename — used only to extract
     *                         the extension
     * @param folder           logical sub-folder, e.g. {@code "vendors"}
     * @return relative path from the storage root
     */
    String store(InputStream data, String originalFilename, String folder) throws IOException;

    /**
     * Delete a previously stored file.
     *
     * @param storedPath relative path returned by {@link #store}
     */
    void delete(String storedPath) throws IOException;

    /**
     * Resolve the absolute filesystem {@link Path} for a relative stored path.
     * Useful for streaming files back to the client.
     */
    Path resolve(String storedPath);

    /**
     * Returns the absolute, normalised root of the uploads directory.
     * Used by {@code FileServeResource} to guard against path-traversal attacks.
     */
    Path getRoot();
}

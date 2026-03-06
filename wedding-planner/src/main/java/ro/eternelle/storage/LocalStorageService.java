package ro.eternelle.storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * {@link StorageService} backed by the local filesystem.
 * <p>
 * Files are written to {@code {app.uploads.dir}/{folder}/{uuid}{ext}}.
 * To switch to S3/R2 create an alternative {@code @ApplicationScoped} bean
 * that implements {@link StorageService}, add
 * {@code @io.quarkus.arc.DefaultBean} here and {@code @Primary} on the new
 * one (or use CDI {@code @Priority}).
 */
@ApplicationScoped
public class LocalStorageService implements StorageService {

    @ConfigProperty(name = "app.uploads.dir", defaultValue = "uploads")
    String uploadsDir;

    @Override
    public String store(InputStream data, String originalFilename, String folder) throws IOException {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            // Clamp potentially dangerous extensions to their mime-safe form
            ext = ext.toLowerCase().replaceAll("[^.a-z0-9]", "");
        }

        String filename     = UUID.randomUUID() + ext;
        String relativePath = folder + "/" + filename;
        Path   target       = Paths.get(uploadsDir).resolve(relativePath);

        Files.createDirectories(target.getParent());
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);

        return relativePath;
    }

    @Override
    public void delete(String storedPath) throws IOException {
        if (storedPath == null) return;
        Files.deleteIfExists(Paths.get(uploadsDir).resolve(storedPath));
    }

    @Override
    public Path resolve(String storedPath) {
        return Paths.get(uploadsDir).resolve(storedPath);
    }

    @Override
    public Path getRoot() {
        return Paths.get(uploadsDir).toAbsolutePath().normalize();
    }
}

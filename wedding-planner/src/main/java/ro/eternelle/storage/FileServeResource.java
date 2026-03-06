package ro.eternelle.storage;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Generic file-serve endpoint for all locally stored uploads.
 *
 * <p>URL pattern: {@code GET /api/uploads/{folder}/{filename}}
 *
 * <p>Example: {@code GET /api/uploads/vendors/3f2a1b.jpg}
 *
 * <p>When you swap {@link LocalStorageService} for an S3/R2 implementation
 * this endpoint can simply redirect to the CDN URL instead.
 */
@Path("/api/uploads")
@PermitAll
public class FileServeResource {

    @Inject
    StorageService storageService;

    @GET
    @Path("/{folder}/{filename}")
    @Produces(MediaType.WILDCARD)
    public Response serve(@PathParam("folder") String folder,
                          @PathParam("filename") String filename) throws IOException {
        // First line: reject obvious traversal attempts in path segments
        if (folder.contains("..") || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        java.nio.file.Path file = storageService.resolve(folder + "/" + filename)
                .toAbsolutePath().normalize();

        // Second line: ensure the resolved path is still within the uploads root
        // (guards against URL-encoded sequences that survive the string check above)
        if (!file.startsWith(storageService.getRoot())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (!Files.exists(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String contentType = Files.probeContentType(file);
        return Response.ok(file.toFile())
                .type(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM)
                .header("Cache-Control", "public, max-age=86400")
                .build();
    }
}

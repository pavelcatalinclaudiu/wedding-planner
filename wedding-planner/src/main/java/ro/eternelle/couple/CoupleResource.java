package ro.eternelle.couple;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.storage.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Path("/api/couples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoupleResource {

    @Inject
    CoupleService coupleService;

    @Inject
    JsonWebToken jwt;

    @Inject
    StorageService storageService;

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private void validateImageUpload(FileUpload file) {
        String ct = file.contentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase())) {
            throw new BusinessException("Invalid file type. Allowed: JPEG, PNG, WebP, GIF");
        }
        if (file.size() > MAX_IMAGE_BYTES) {
            throw new BusinessException("File too large. Maximum size is 5 MB");
        }
    }

    private String toUrl(String storagePath) {
        return "/api/uploads/" + storagePath;
    }

    private String toStoragePath(String url) {
        return url.replace("/api/uploads/", "");
    }

    @GET
    @Path("/me")
    @RolesAllowed("COUPLE")
    public Response getMyProfile() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(coupleService.getByUserId(userId)).build();
    }

    @POST
    @RolesAllowed("COUPLE")
    public Response createProfile(CoupleProfile profile) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(coupleService.createProfile(userId, profile))
                .build();
    }

    @PUT
    @Path("/me")
    @RolesAllowed("COUPLE")
    public Response updateProfile(CoupleProfile updates) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(coupleService.updateProfile(userId, updates)).build();
    }

    @POST
    @Path("/me/picture")
    @RolesAllowed("COUPLE")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadPicture(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");
        validateImageUpload(file);
        UUID userId = UUID.fromString(jwt.getSubject());
        CoupleProfile couple = coupleService.getByUserId(userId);
        if (couple.profilePicture != null) {
            storageService.delete(toStoragePath(couple.profilePicture));
        }
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            String storagePath = storageService.store(is, file.fileName(), "couples");
            couple.profilePicture = toUrl(storagePath);
        }
        return Response.ok(Map.of("profilePicture", couple.profilePicture)).build();
    }

    @DELETE
    @Path("/me/picture")
    @RolesAllowed("COUPLE")
    @Transactional
    public Response deletePicture() throws IOException {
        UUID userId = UUID.fromString(jwt.getSubject());
        CoupleProfile couple = coupleService.getByUserId(userId);
        if (couple.profilePicture != null) {
            storageService.delete(toStoragePath(couple.profilePicture));
            couple.profilePicture = null;
        }
        return Response.noContent().build();
    }
}

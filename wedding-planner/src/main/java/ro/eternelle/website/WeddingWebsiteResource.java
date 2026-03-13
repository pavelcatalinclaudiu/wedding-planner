package ro.eternelle.website;

import jakarta.annotation.security.PermitAll;
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
import ro.eternelle.guest.GuestService;
import ro.eternelle.storage.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;

@Path("/api/couples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WeddingWebsiteResource {

    @Inject WeddingWebsiteService websiteService;
    @Inject GuestService guestService;
    @Inject StorageService storageService;
    @Inject JsonWebToken jwt;

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    // ── Authenticated couple endpoints ─────────────────────────────────────

    @GET
    @Path("/me/website")
    @RolesAllowed("COUPLE")
    public Response getMyWebsite() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(websiteService.getOrCreate(userId)).build();
    }

    @PUT
    @Path("/me/website")
    @RolesAllowed("COUPLE")
    public Response updateMyWebsite(WeddingWebsiteDTO.UpdateRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(websiteService.update(userId, req)).build();
    }

    @POST
    @Path("/me/website/photo")
    @RolesAllowed("COUPLE")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadCoverPhoto(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");
        String ct = file.contentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct.toLowerCase()))
            throw new BusinessException("Invalid file type. Allowed: JPEG, PNG, WebP");
        if (file.size() > MAX_IMAGE_BYTES)
            throw new BusinessException("File too large. Maximum size is 5 MB");

        UUID userId = UUID.fromString(jwt.getSubject());
        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            String storagePath = storageService.store(is, file.fileName(), "wedding-websites");
            String url = "/api/uploads/" + storagePath;
            return Response.ok(websiteService.updateCoverPhoto(userId, url)).build();
        }
    }

    @GET
    @Path("/me/website/preview")
    @RolesAllowed("COUPLE")
    public Response previewMyWebsite() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(websiteService.getPreview(userId)).build();
    }

    // ── Public endpoints ───────────────────────────────────────────────────

    @GET
    @Path("/website/{subdomain}")
    @PermitAll
    public Response getPublicWebsite(@PathParam("subdomain") String subdomain) {
        return Response.ok(websiteService.getPublic(subdomain)).build();
    }

    @POST
    @Path("/website/{subdomain}/rsvp")
    @PermitAll
    public Response submitRsvp(
            @PathParam("subdomain") String subdomain,
            RsvpRequest req) {
        guestService.submitPublicRsvp(subdomain, req);
        return Response.ok().build();
    }

    @POST
    @Path("/website/{subdomain}/visit")
    @PermitAll
    public Response trackVisit(@PathParam("subdomain") String subdomain) {
        websiteService.trackVisit(subdomain);
        return Response.noContent().build();
    }
}

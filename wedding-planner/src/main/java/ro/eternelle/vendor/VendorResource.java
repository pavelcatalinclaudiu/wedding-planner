package ro.eternelle.vendor;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/vendors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorResource {

    @Inject
    VendorService vendorService;

    @Inject
    VendorAnalyticsService vendorAnalyticsService;

    @Inject
    AvailabilityRepository availabilityRepository;

    @Inject
    ro.eternelle.booking.BookingRepository bookingRepository;

    @Inject
    ProfileViewService profileViewService;

    @Inject
    StorageService storageService;

    @Inject
    JsonWebToken jwt;

    @GET
    @PermitAll
    public Response search(
            @QueryParam("category") String category,
            @QueryParam("city") String city,
            @QueryParam("q") String keyword,
            @QueryParam("date") String date) {
        return Response.ok(vendorService.search(category, city, keyword, date)).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getVendor(@PathParam("id") UUID id,
                              @Context jakarta.ws.rs.core.HttpHeaders httpHeaders) {
        // Resolve viewer ID (null for unauthenticated users)
        UUID viewerId = null;
        try {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) viewerId = UUID.fromString(sub);
        } catch (Exception ignored) {}

        // Extract client IP from forwarding header with fallback
        String rawIp = httpHeaders.getHeaderString("X-Forwarded-For");
        if (rawIp == null) rawIp = httpHeaders.getHeaderString("X-Real-IP");

        // Fire view tracking (runs in REQUIRES_NEW transaction, silently no-ops on error)
        profileViewService.recordView(id, viewerId, rawIp);

        return Response.ok(vendorService.getById(id)).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed("VENDOR")
    public Response getMyProfile() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(vendorService.getByUserId(userId)).build();
    }

    @POST
    @RolesAllowed("VENDOR")
    public Response createProfile(VendorProfile profile) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(vendorService.createProfile(userId, profile))
                .build();
    }

    @GET
    @Path("/analytics")
    @RolesAllowed("VENDOR")
    public Response getAnalytics() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(vendorService.getAnalytics(userId)).build();
    }

    @GET
    @Path("/analytics/deep")
    @RolesAllowed("VENDOR")
    public Response getDeepAnalytics() {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        return Response.ok(vendorAnalyticsService.getDeepAnalytics(vendor.id)).build();
    }

    // ── Public stats ──────────────────────────────────────────────────────────

    @GET
    @Path("/{id}/stats")
    @PermitAll
    public Response getVendorStats(@PathParam("id") UUID id) {
        double avg = vendorService.computeAvgResponseTimeHours(id);
        return Response.ok(java.util.Map.of("avgResponseTimeHours", avg)).build();
    }

    // ── Availability ──────────────────────────────────────────────────────────

    @GET
    @Path("/{id}/availability")
    @PermitAll
    public Response getVendorAvailability(@PathParam("id") UUID id) {
        // Manual blocks
        List<AvailabilityDTO> list = availabilityRepository.findByVendor(id)
                .stream().map(AvailabilityDTO::from).collect(Collectors.toList());
        // Confirmed booking dates — also block these on the enquiry calendar
        java.util.Set<String> existingDates = list.stream()
                .map(d -> d.date).collect(java.util.stream.Collectors.toSet());
        bookingRepository.findConfirmedWeddingDatesByVendor(id).forEach(date -> {
            String dateStr = date.toString();
            if (!existingDates.contains(dateStr)) {
                AvailabilityDTO dto = new AvailabilityDTO();
                dto.vendorId = id;
                dto.date = dateStr;
                dto.reason = "BOOKING";
                list.add(dto);
            }
        });
        return Response.ok(list).build();
    }

    @GET
    @Path("/me/availability")
    @RolesAllowed("VENDOR")
    public Response getMyAvailability() {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        List<AvailabilityDTO> list = availabilityRepository.findByVendor(vendor.id)
                .stream().map(AvailabilityDTO::from).collect(Collectors.toList());
        return Response.ok(list).build();
    }

    @POST
    @Path("/me/availability/block")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response blockDate(BlockDateRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        LocalDate date = LocalDate.parse(req.date);
        availabilityRepository.findByVendorAndDate(vendor.id, date).ifPresentOrElse(
            a -> {}, // already blocked, no-op
            () -> {
                VendorAvailability block = new VendorAvailability();
                block.vendor = vendor;
                block.date = date;
                block.reason = req.reason;
                availabilityRepository.persist(block);
            }
        );
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/me/availability/block/{date}")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response unblockDate(@PathParam("date") String dateStr) {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        LocalDate date = LocalDate.parse(dateStr);
        availabilityRepository.findByVendorAndDate(vendor.id, date)
                .ifPresent(a -> availabilityRepository.delete(a));
        return Response.noContent().build();
    }

    public static class BlockDateRequest {
        public String date;
        public String reason;
    }

    @PATCH
    @Path("/me/visibility")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response toggleVisibility() {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        vendor.isActive = !vendor.isActive;
        return Response.ok(Map.of("active", vendor.isActive)).build();
    }

    @PUT
    @Path("/me")
    @RolesAllowed("VENDOR")
    public Response updateProfile(VendorProfile updates) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(vendorService.updateProfile(userId, updates)).build();
    }

    // ── Profile picture ───────────────────────────────────────────────────────

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024; // 5 MB

    private void validateImageUpload(FileUpload file) {
        String ct = file.contentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct.toLowerCase())) {
            throw new BusinessException("Invalid file type. Allowed: JPEG, PNG, WebP, GIF");
        }
        if (file.size() > MAX_IMAGE_BYTES) {
            throw new BusinessException("File too large. Maximum size is 5 MB");
        }
    }

    @POST
    @Path("/me/picture")
    @RolesAllowed("VENDOR")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadPicture(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");
        validateImageUpload(file);
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);

        // Remove previous picture from storage if present
        if (vendor.profilePicture != null) {
            storageService.delete(toStoragePath(vendor.profilePicture));
        }

        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            String storagePath = storageService.store(is, file.fileName(), "vendors");
            vendor.profilePicture = toUrl(storagePath);
        }

        return Response.ok(Map.of("profilePicture", vendor.profilePicture)).build();
    }

    @DELETE
    @Path("/me/picture")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response deletePicture() throws IOException {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        if (vendor.profilePicture != null) {
            storageService.delete(toStoragePath(vendor.profilePicture));
            vendor.profilePicture = null;
        }
        return Response.noContent().build();
    }

    // ── Photo gallery ─────────────────────────────────────────────────────────

    private static final String UPLOADS_URL_PREFIX = "/api/uploads/";

    private String toUrl(String storagePath) {
        return UPLOADS_URL_PREFIX + storagePath;
    }

    private String toStoragePath(String url) {
        return url != null && url.startsWith(UPLOADS_URL_PREFIX)
                ? url.substring(UPLOADS_URL_PREFIX.length())
                : url;
    }

    @POST
    @Path("/me/photos")
    @RolesAllowed("VENDOR")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadPhoto(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");
        validateImageUpload(file);
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);

        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            String storagePath = storageService.store(is, file.fileName(), "vendors");
            String url = toUrl(storagePath);

            VendorPhoto photo = new VendorPhoto();
            photo.vendor = vendor;
            photo.url = url;
            photo.sortOrder = (int) VendorPhoto.count("vendor.id", vendor.id);
            photo.persist();

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("url", url, "id", photo.id.toString(), "order", photo.sortOrder))
                    .build();
        }
    }

    @DELETE
    @Path("/me/photos/{photoId}")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response deletePhoto(@PathParam("photoId") UUID photoId) throws IOException {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);

        VendorPhoto photo = VendorPhoto.findById(photoId);
        if (photo == null || !photo.vendor.id.equals(vendor.id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        storageService.delete(toStoragePath(photo.url));
        photo.delete();
        return Response.noContent().build();
    }

    @PATCH
    @Path("/me/photos/reorder")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response reorderPhotos(ReorderRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);

        for (int i = 0; i < req.order.size(); i++) {
            UUID photoId = UUID.fromString(req.order.get(i));
            VendorPhoto photo = VendorPhoto.findById(photoId);
            if (photo != null && photo.vendor.id.equals(vendor.id)) {
                photo.sortOrder = i;
            }
        }
        return Response.ok().build();
    }

    public static class ReorderRequest {
        public List<String> order;
    }

    // ── Cover photo ───────────────────────────────────────────────────────────

    @POST
    @Path("/me/cover")
    @RolesAllowed("VENDOR")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadCover(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");
        validateImageUpload(file);
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);

        if (vendor.coverPhoto != null) {
            storageService.delete(toStoragePath(vendor.coverPhoto));
        }

        try (InputStream is = Files.newInputStream(file.uploadedFile())) {
            String storagePath = storageService.store(is, file.fileName(), "vendors");
            vendor.coverPhoto = toUrl(storagePath);
        }

        return Response.ok(Map.of("url", vendor.coverPhoto)).build();
    }

    @DELETE
    @Path("/me/cover")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response deleteCover() throws IOException {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile vendor = vendorService.getByUserId(userId);
        if (vendor.coverPhoto != null) {
            storageService.delete(toStoragePath(vendor.coverPhoto));
            vendor.coverPhoto = null;
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("VENDOR")
    public Response updateProfileById(@PathParam("id") UUID id, VendorProfile updates) {
        UUID userId = UUID.fromString(jwt.getSubject());
        VendorProfile existing = vendorService.getById(id);
        if (!existing.user.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(vendorService.updateProfile(userId, updates)).build();
    }
}

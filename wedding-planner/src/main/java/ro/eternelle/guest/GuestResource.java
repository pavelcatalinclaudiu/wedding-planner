package ro.eternelle.guest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/guests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("COUPLE")
public class GuestResource {

    @Inject GuestService guestService;
    @Inject JsonWebToken jwt;

    // ── List guests ────────────────────────────────────────────────────

    @GET
    public Response getGuests() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(guestService.getGuests(userId)).build();
    }

    // ── Stats ─────────────────────────────────────────────────────────

    @GET
    @Path("/stats")
    public Response getStats() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(guestService.getStats(userId)).build();
    }

    // ── Song requests ────────────────────────────────────────────────

    @GET
    @Path("/songs")
    public Response getSongRequests() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(guestService.getSongRequests(userId)).build();
    }

    // ── CSV Export ────────────────────────────────────────────────────

    @GET
    @Path("/export")
    @Produces("text/csv")
    public Response exportCsv() {
        UUID userId = UUID.fromString(jwt.getSubject());
        String csv = guestService.exportCsv(userId);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"guests.csv\"")
                .build();
    }

    // ── Create guest ──────────────────────────────────────────────────

    @POST
    public Response addGuest(GuestRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GuestDTO created = guestService.addGuest(userId, req);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    // ── Update guest ──────────────────────────────────────────────────

    @PUT
    @Path("/{id}")
    public Response updateGuest(@PathParam("id") UUID id, GuestRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(guestService.updateGuest(id, userId, req)).build();
    }

    // ── Delete guest ────────────────────────────────────

    @DELETE
    @Path("/{id}")
    public Response deleteGuest(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        guestService.deleteGuest(id, userId);
        return Response.noContent().build();
    }

    // ── CSV Import ────────────────────────────────────────────────────

    @POST
    @Path("/import")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response importCsv(String csvBody) {
        UUID userId = UUID.fromString(jwt.getSubject());
        int imported = guestService.importCsv(userId, csvBody);
        return Response.ok(java.util.Map.of("imported", imported)).build();
    }
}

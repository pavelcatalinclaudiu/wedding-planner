package ro.eternelle.notification;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.UUID;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class NotificationResource {

    @Inject NotificationService notificationService;
    @Inject JsonWebToken jwt;

    @GET
    public Response getAll() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(notificationService.getForUser(userId)).build();
    }

    @GET
    @Path("/unread-count")
    public Response countUnread() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(Map.of("count", notificationService.countUnread(userId))).build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markRead(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/read-all")
    public Response markAllRead() {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markAllRead(userId);
        return Response.noContent().build();
    }
}

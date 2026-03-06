package ro.eternelle.videocall;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.UUID;

@Path("/api/video-calls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class VideoCallResource {

    @Inject VideoCallService videoCallService;
    @Inject JsonWebToken     jwt;
    @Inject JaasTokenService jaasTokenService;

    @GET
    public Response list() {
        UUID userId      = UUID.fromString(jwt.getSubject());
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        return Response.ok(
            videoCallService.getForUser(userId, isVendor).stream()
                .map(VideoCallDTO::from).toList()
        ).build();
    }

    @GET
    @Path("/lead/{leadId}")
    public Response getByLead(@PathParam("leadId") UUID leadId) {
        return Response.ok(
            videoCallService.getByLead(leadId).stream()
                .map(VideoCallDTO::from).toList()
        ).build();
    }

    @POST
    @Path("/lead/{leadId}/schedule")
    public Response schedule(
            @PathParam("leadId") UUID leadId,
            @QueryParam("scheduledAt") String scheduledAt,
            @QueryParam("postCallAction") PostCallAction action) {
        Instant time     = Instant.parse(scheduledAt);
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        VideoCall call   = videoCallService.schedule(leadId, time, action, isVendor);
        String  token    = isVendor
                ? jaasTokenService.moderatorToken(call.roomName, "Vendor")
                : jaasTokenService.participantToken(call.roomName, "Couple");
        return Response.status(Response.Status.CREATED)
                .entity(VideoCallDTO.from(call, token))
                .build();
    }

    @POST
    @Path("/{id}/accept")
    public Response accept(@PathParam("id") UUID id) {
        UUID userId      = UUID.fromString(jwt.getSubject());
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        VideoCall call   = videoCallService.accept(id, userId);
        String token     = isVendor
                ? jaasTokenService.moderatorToken(call.roomName, "Vendor")
                : jaasTokenService.participantToken(call.roomName, "Couple");
        return Response.ok(VideoCallDTO.from(call, token)).build();
    }

    @POST
    @Path("/{id}/start")
    public Response start(@PathParam("id") UUID id) {
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        VideoCall call   = videoCallService.start(id);
        String  token    = isVendor
                ? jaasTokenService.moderatorToken(call.roomName, "Vendor")
                : jaasTokenService.participantToken(call.roomName, "Couple");
        return Response.ok(VideoCallDTO.from(call, token)).build();
    }

    @POST
    @Path("/{id}/end")
    public Response end(@PathParam("id") UUID id) {
        return Response.ok(VideoCallDTO.from(videoCallService.end(id))).build();
    }

    @PATCH
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(VideoCallDTO.from(videoCallService.cancel(id, userId))).build();
    }

    @PATCH
    @Path("/{id}/reschedule")
    public Response reschedule(
            @PathParam("id") UUID id,
            @QueryParam("scheduledAt") String scheduledAt) {
        UUID userId      = UUID.fromString(jwt.getSubject());
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        Instant time     = Instant.parse(scheduledAt);
        return Response.ok(VideoCallDTO.from(videoCallService.reschedule(id, time, userId, isVendor))).build();
    }

    /** Returns the active (SCHEDULED or IN_PROGRESS) call for a lead, or 204 if none. */
    @GET
    @Path("/lead/{leadId}/active")
    public Response getActiveForLead(@PathParam("leadId") UUID leadId) {
        boolean isVendor = jwt.getGroups().contains("VENDOR");
        return videoCallService.getActiveForLead(leadId)
                .map(c -> {
                    String token = isVendor
                            ? jaasTokenService.moderatorToken(c.roomName, "Vendor")
                            : jaasTokenService.participantToken(c.roomName, "Couple");
                    return Response.ok(VideoCallDTO.from(c, token)).build();
                })
                .orElse(Response.noContent().build());
    }
}

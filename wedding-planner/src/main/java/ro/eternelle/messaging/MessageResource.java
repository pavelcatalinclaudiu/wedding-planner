package ro.eternelle.messaging;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class MessageResource {

    @Inject MessageService messageService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/threads")
    public Response getMyThreads() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(messageService.getThreadsForUser(userId)).build();
    }

    @GET
    @Path("/threads/lead/{leadId}")
    public Response getOrCreateThread(@PathParam("leadId") UUID leadId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(messageService.getOrCreateThread(leadId, userId)).build();
    }

    @GET
    @Path("/threads/{threadId}")
    public Response getMessages(
            @PathParam("threadId") UUID threadId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        return Response.ok(messageService.getMessages(threadId, page, size)).build();
    }

    @POST
    @Path("/threads/{threadId}/send")
    public Response sendMessage(@PathParam("threadId") UUID threadId, SendMessageRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(messageService.sendMessage(threadId, userId, req.content, req.type))
                .build();
    }

    @POST
    @Path("/threads/{threadId}/read")
    public Response markRead(@PathParam("threadId") UUID threadId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        messageService.markRead(threadId, userId);
        return Response.noContent().build();
    }

    // ─── Group chat endpoints ─────────────────────────────────────────────────

    /**
     * GET /api/messages/group
     * COUPLE → returns the single group thread for their wedding
     * VENDOR → returns a list of group threads they participate in
     */
    @GET
    @Path("/group")
    public Response getGroupChat() {
        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaim("role");

        if ("COUPLE".equals(role)) {
            return Response.ok(messageService.getGroupThreadForCouple(userId)).build();
        } else {
            return Response.ok(messageService.getGroupThreadsForVendor(userId)).build();
        }
    }

    /** GET /api/messages/threads/{threadId}/participants */
    @GET
    @Path("/threads/{threadId}/participants")
    public Response getParticipants(@PathParam("threadId") UUID threadId) {
        return Response.ok(messageService.getParticipants(threadId)).build();
    }

    /**
     * POST /api/messages/threads/{threadId}/participants
     * Couple manually adds a booked vendor to the group chat.
     */
    @POST
    @Path("/threads/{threadId}/participants")
    @RolesAllowed("COUPLE")
    public Response addParticipant(
            @PathParam("threadId") UUID threadId,
            AddParticipantRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        messageService.addVendorToGroupThread(threadId, req.vendorId, userId);
        return Response.noContent().build();
    }

    /**
     * DELETE /api/messages/threads/{threadId}/participants/{participantId}
     * Only the couple owner may call this.
     */
    @DELETE
    @Path("/threads/{threadId}/participants/{participantId}")
    @RolesAllowed("COUPLE")
    public Response removeParticipant(
            @PathParam("threadId") UUID threadId,
            @PathParam("participantId") UUID participantId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        messageService.removeParticipant(threadId, participantId, userId);
        return Response.noContent().build();
    }

    /**
     * POST /api/messages/threads/{threadId}/leave
     * Vendor voluntarily leaves a group thread.
     */
    @POST
    @Path("/threads/{threadId}/leave")
    @RolesAllowed("VENDOR")
    public Response leaveThread(@PathParam("threadId") UUID threadId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        messageService.leaveThread(threadId, userId);
        return Response.noContent().build();
    }

    // ─── Inner request classes ────────────────────────────────────────────────

    public static class SendMessageRequest {
        public String content;
        public MessageType type;
    }

    public static class AddParticipantRequest {
        public UUID vendorId;
    }
}

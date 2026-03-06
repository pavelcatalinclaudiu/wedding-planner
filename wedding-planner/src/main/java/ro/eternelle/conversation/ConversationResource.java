package ro.eternelle.conversation;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.UUID;

@Path("/api/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class ConversationResource {

    @Inject ConversationService conversationService;
    @Inject JsonWebToken jwt;

    /** GET /api/conversations/lead/{leadId} — fetch conversation for a lead */
    @GET
    @Path("/lead/{leadId}")
    public Response getByLead(@PathParam("leadId") UUID leadId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(conversationService.getByLead(leadId, userId)).build();
    }

    /** GET /api/conversations/my — list all my conversations */
    @GET
    @Path("/my")
    public Response getMine() {
        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaim("role");
        return Response.ok(conversationService.getMyConversations(userId, role)).build();
    }

    /** GET /api/conversations/{id}/messages — message history */
    @GET
    @Path("/{id}/messages")
    public Response getMessages(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(conversationService.getMessages(id, userId)).build();
    }

    /** POST /api/conversations/{id}/messages — send a message */
    @POST
    @Path("/{id}/messages")
    public Response sendMessage(@PathParam("id") UUID id, Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String role    = jwt.getClaim("role");
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return Response.status(400).entity(Map.of("error", "content is required")).build();
        }
        ConversationMessageDTO dto = conversationService.sendMessage(id, userId, role, content);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }
}

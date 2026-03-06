package ro.eternelle.checklist;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/checklist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("COUPLE")
public class ChecklistResource {

    @Inject ChecklistService checklistService;
    @Inject JsonWebToken jwt;

    @GET
    public Response getItems() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(checklistService.getByCouple(userId)).build();
    }

    @POST
    public Response addItem(ChecklistItemRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(checklistService.addItem(userId, req))
                .build();
    }

    @POST
    @Path("/{id}/toggle")
    public Response toggle(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(checklistService.toggleComplete(id, userId)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, ChecklistItemRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(checklistService.updateItem(id, userId, req)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        checklistService.deleteItem(id, userId);
        return Response.noContent().build();
    }
}

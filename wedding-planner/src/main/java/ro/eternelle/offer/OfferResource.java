package ro.eternelle.offer;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/offers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class OfferResource {

    @Inject OfferService offerService;
    @Inject JsonWebToken jwt;

    /** POST /api/offers — vendor creates an offer for a lead */
    @POST
    @RolesAllowed("VENDOR")
    public Response create(CreateOfferRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(offerService.createOffer(req.leadId, userId, req))
                .build();
    }

    /** GET /api/offers?leadId=… — list offers for a lead */
    @GET
    public Response getByLead(@QueryParam("leadId") UUID leadId) {
        if (leadId == null) return Response.status(400).build();
        return Response.ok(offerService.getByLead(leadId)).build();
    }

    /** PATCH /api/offers/{id}/accept — couple accepts offer */
    @PATCH
    @Path("/{id}/accept")
    @RolesAllowed("COUPLE")
    public Response accept(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(offerService.acceptOffer(id, userId)).build();
    }

    /** PATCH /api/offers/{id}/decline — couple declines offer */
    @PATCH
    @Path("/{id}/decline")
    @RolesAllowed("COUPLE")
    public Response decline(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(offerService.declineOffer(id, userId)).build();
    }

    /** PATCH /api/offers/{id}/request-revision – couple requests a revised offer */
    @PATCH
    @Path("/{id}/request-revision")
    @RolesAllowed("COUPLE")
    public Response requestRevision(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(offerService.requestRevision(id, userId)).build();
    }
}

package ro.eternelle.couple;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/couples")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoupleResource {

    @Inject
    CoupleService coupleService;

    @Inject
    JsonWebToken jwt;

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
}

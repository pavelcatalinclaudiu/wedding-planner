package ro.eternelle.review;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/api/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewResource {

    @Inject ReviewService reviewService;
    @Inject JsonWebToken jwt;

    @GET
    @RolesAllowed("VENDOR")
    public Response getMyReviews() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(reviewService.getByVendorUser(userId)).build();
    }

    @GET
    @Path("/vendor/{vendorId}")
    @PermitAll
    public Response getByVendor(@PathParam("vendorId") UUID vendorId) {
        return Response.ok(reviewService.getByVendor(vendorId)).build();
    }

    @PATCH
    @Path("/{id}/reply")
    @RolesAllowed("VENDOR")
    public Response addReply(@PathParam("id") UUID reviewId, ReplyRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(reviewService.addReply(reviewId, userId, req.reply)).build();
    }

    public static class ReplyRequest {
        public String reply;
    }

    @POST
    @Path("/booking/{bookingId}")
    @RolesAllowed("COUPLE")
    public Response createReview(
            @PathParam("bookingId") UUID bookingId,
            CreateReviewRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(reviewService.createReview(bookingId, userId, req.rating, req.comment))
                .build();
    }

    public static class CreateReviewRequest {
        public BigDecimal rating;
        public String comment;
    }
}

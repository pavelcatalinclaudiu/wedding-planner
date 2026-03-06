package ro.eternelle.subscription;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.UUID;

@Path("/api/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class SubscriptionResource {

    @Inject SubscriptionService subscriptionService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/me")
    public Response getMySubscription() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return subscriptionService.getActiveSubscription(userId)
                .map(s -> Response.ok(s).build())
                .orElse(Response.status(404).build());
    }

    @POST
    @Path("/checkout")
    public Response createCheckout(
            @QueryParam("priceId") String priceId,
            @QueryParam("successUrl") String successUrl,
            @QueryParam("cancelUrl") String cancelUrl) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String url = subscriptionService.createCheckoutSession(userId, priceId, successUrl, cancelUrl);
        return Response.ok(Map.of("checkoutUrl", url)).build();
    }
}

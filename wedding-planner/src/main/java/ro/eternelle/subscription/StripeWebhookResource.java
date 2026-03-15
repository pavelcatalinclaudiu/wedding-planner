package ro.eternelle.subscription;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/webhooks/stripe")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StripeWebhookResource {

    @Inject
    SubscriptionService subscriptionService;

    @POST
    public Response handle(String payload, @HeaderParam("Stripe-Signature") String sigHeader) {
        try {
            subscriptionService.handleWebhook(payload, sigHeader);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(400).entity("Webhook error: " + e.getMessage()).build();
        }
    }
}

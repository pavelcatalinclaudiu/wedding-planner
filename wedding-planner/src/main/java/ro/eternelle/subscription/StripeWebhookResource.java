package ro.eternelle.subscription;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.UUID;

@Path("/api/webhooks/stripe")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StripeWebhookResource {

    @ConfigProperty(name = "stripe.webhook.secret")
    Optional<String> webhookSecret;

    @Inject
    SubscriptionService subscriptionService;

    @POST
    public Response handle(String payload, @HeaderParam("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret.orElseThrow(() -> new RuntimeException("Stripe webhook secret not configured")));

            switch (event.getType()) {
                case "customer.subscription.created" -> {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    String userId = sub.getMetadata().get("userId");
                    subscriptionService.handleSubscriptionCreated(
                            sub.getId(),
                            sub.getCustomer(),
                            sub.getItems().getData().get(0).getPrice().getId(),
                            sub.getItems().getData().get(0).getPrice().getNickname(),
                            sub.getStatus(),
                            UUID.fromString(userId),
                            sub.getCurrentPeriodStart(),
                            sub.getCurrentPeriodEnd()
                    );
                }
                case "customer.subscription.updated" -> {
                    Subscription sub = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    subscriptionService.handleSubscriptionUpdated(
                            sub.getId(), sub.getStatus(), sub.getCancelAtPeriodEnd());
                }
                default -> { /* unhandled event */ }
            }

            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(400).entity("Webhook error: " + e.getMessage()).build();
        }
    }
}

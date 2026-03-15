package ro.eternelle.subscription;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriptionResource {

    @Inject SubscriptionService subscriptionService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/me")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response getMySubscription() {
        UUID userId = UUID.fromString(jwt.getSubject());
        // First try real Stripe subscription; fall back to admin-set profile tier
        Optional<Subscription> sub = subscriptionService.getActiveSubscription(userId);
        if (sub.isEmpty()) {
            sub = subscriptionService.getProfileSubscription(userId);
        }
        return sub.map(s -> Response.ok(s).build()).orElse(Response.ok(null).build());
    }

    /**
     * Initiate a Stripe checkout session.
     * Body: { "plan": "STANDARD" | "PREMIUM" | "DREAM_WEDDING", "billing": "monthly" | "onetime" }
     * Returns: { "url": "https://checkout.stripe.com/..." }
     */
    @POST
    @Path("/checkout")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response createCheckout(Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String plan = body.getOrDefault("plan", "");
        String billing = body.getOrDefault("billing", "monthly");
        String successUrl = body.getOrDefault("successUrl", "http://localhost:5173/vendor/subscription?success=true");
        String cancelUrl = body.getOrDefault("cancelUrl", "http://localhost:5173/vendor/subscription");
        String url = subscriptionService.createCheckoutSession(userId, plan, billing, successUrl, cancelUrl);
        return Response.ok(Map.of("url", url)).build();
    }

    /**
     * Create a Stripe billing portal session.
     * Returns: { "url": "https://billing.stripe.com/..." }
     */
    @POST
    @Path("/portal")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response createPortal(Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String returnUrl = body.getOrDefault("returnUrl", "http://localhost:5173/vendor/subscription");
        String url = subscriptionService.createPortalSession(userId, returnUrl);
        return Response.ok(Map.of("url", url)).build();
    }

    /**
     * Upgrade or downgrade an existing Stripe subscription in-place.
     * Stripe prorates the billing difference immediately.
     * Body: { "plan": "STANDARD" | "PREMIUM" }
     */
    @POST
    @Path("/upgrade")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response upgradeSubscription(Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String plan = body.getOrDefault("plan", "");
        if (plan.isBlank()) {
            return Response.status(400).entity(Map.of("error", "plan required")).build();
        }
        Subscription sub = subscriptionService.upgradeSubscription(userId, plan);
        return Response.ok(sub).build();
    }

    /**
     * Cancel subscription at period end.
     */
    @POST
    @Path("/cancel")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response cancelSubscription() {
        UUID userId = UUID.fromString(jwt.getSubject());
        subscriptionService.cancelSubscription(userId);
        return Response.ok(Map.of("cancelled", true)).build();
    }

    /**
     * Sync subscription from a completed Stripe Checkout Session.
     * Called by the frontend on the success redirect page.
     * Body: { "sessionId": "cs_..." }
     */
    @POST
    @Path("/sync-session")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response syncSession(Map<String, String> body) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String sessionId = body.getOrDefault("sessionId", "");
        if (sessionId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "sessionId required")).build();
        }
        subscriptionService.syncFromCheckoutSession(userId, sessionId);
        return subscriptionService.getActiveSubscription(userId)
                .map(s -> Response.ok(s).build())
                .orElse(Response.ok(null).build());
    }

    /**
     * Background sync: check Stripe directly for any active subscription for this user.
     * Call this on page load when DB shows no paid plan, to catch missed webhooks/redirects.
     */
    @POST
    @Path("/sync")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response sync() {
        UUID userId = UUID.fromString(jwt.getSubject());
        subscriptionService.syncFromStripe(userId);
        return subscriptionService.getActiveSubscription(userId)
                .map(s -> Response.ok(s).build())
                .orElse(Response.ok(null).build());
    }

    /**
     * Stripe webhook endpoint — must be PermitAll, raw body for signature verification.
     */
    @POST
    @Path("/webhook")
    @PermitAll
    @Consumes(MediaType.TEXT_PLAIN)
    public Response handleWebhook(String payload,
                                   @HeaderParam("Stripe-Signature") String sigHeader) {
        subscriptionService.handleWebhook(payload, sigHeader);
        return Response.ok().build();
    }
}

package ro.eternelle.subscription;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Event;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ro.eternelle.couple.CouplePlan;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.user.UserRole;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.vendor.VendorTier;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionService {

    @ConfigProperty(name = "stripe.secret.key", defaultValue = "")
    String stripeSecretKey;

    @ConfigProperty(name = "stripe.webhook.secret", defaultValue = "")
    String stripeWebhookSecret;

    @ConfigProperty(name = "stripe.price.vendor.standard")
    Optional<String> priceVendorStandard;

    @ConfigProperty(name = "stripe.price.vendor.premium")
    Optional<String> priceVendorPremium;

    @ConfigProperty(name = "stripe.price.couple.dream_wedding.monthly")
    Optional<String> priceCoupleDreamMonthly;

    @ConfigProperty(name = "stripe.price.couple.dream_wedding.onetime")
    Optional<String> priceCoupleDreamOnetime;

    @Inject SubscriptionRepository subscriptionRepository;
    @Inject UserRepository userRepository;
    @Inject VendorRepository vendorRepository;
    @Inject CoupleRepository coupleRepository;

    private void initStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new BusinessException("Stripe is not configured");
        }
        Stripe.apiKey = stripeSecretKey;
    }

    public Optional<Subscription> getActiveSubscription(UUID userId) {
        return subscriptionRepository.findByUser(userId);
    }

    /**
     * Synthesises a virtual (non-persisted) Subscription from the user's profile
     * tier / plan. Used as a fallback when no Stripe subscription record exists
     * but an admin has manually upgraded the profile.
     */
    public Optional<Subscription> getProfileSubscription(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) return Optional.empty();
        String plan = "FREE";
        if (user.role == UserRole.VENDOR) {
            plan = vendorRepository.findByUserId(userId)
                    .map(v -> v.tier != null ? v.tier.name() : "FREE")
                    .orElse("FREE");
        } else if (user.role == UserRole.COUPLE) {
            plan = coupleRepository.findByUserId(userId)
                    .map(c -> c.plan != null ? c.plan.name() : "FREE")
                    .orElse("FREE");
        }
        if ("FREE".equals(plan)) return Optional.empty();
        Subscription s = new Subscription();
        s.user = user;
        s.planName = plan;
        s.status = "active";
        return Optional.of(s);
    }

    private String resolvePriceId(String plan, String billing) {
        return switch (plan.toUpperCase()) {
            case "STANDARD" -> priceVendorStandard.orElse("");
            case "PREMIUM" -> priceVendorPremium.orElse("");
            case "DREAM_WEDDING" -> "onetime".equals(billing)
                    ? priceCoupleDreamOnetime.orElse("")
                    : priceCoupleDreamMonthly.orElse("");
            default -> throw new BusinessException("Unknown plan: " + plan);
        };
    }

    private String resolvePlanFromPriceId(String priceId) {
        if (priceId == null) return "UNKNOWN";
        if (priceId.equals(priceVendorStandard.orElse(""))) return "STANDARD";
        if (priceId.equals(priceVendorPremium.orElse(""))) return "PREMIUM";
        if (priceId.equals(priceCoupleDreamMonthly.orElse(""))) return "DREAM_WEDDING";
        if (priceId.equals(priceCoupleDreamOnetime.orElse(""))) return "DREAM_WEDDING";
        return "UNKNOWN";
    }

    public String createCheckoutSession(UUID userId, String plan, String billing, String successUrl, String cancelUrl) {
        initStripe();
        User user = userRepository.findById(userId);
        if (user == null) throw new BusinessException("User not found");

        String priceId = resolvePriceId(plan, billing);
        if (priceId == null || priceId.isBlank()) {
            throw new BusinessException("Stripe price not configured for plan: " + plan);
        }

        boolean isOneTime = "onetime".equals(billing);
        try {
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(isOneTime ? SessionCreateParams.Mode.PAYMENT : SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(user.email)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("userId", userId.toString())
                    .putMetadata("plan", plan.toUpperCase());

            if (!isOneTime) {
                builder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("userId", userId.toString())
                        .putMetadata("plan", plan.toUpperCase())
                        .build()
                );
            }

            Session session = Session.create(builder.build());
            return session.getUrl();
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to create checkout session: " + e.getMessage());
        }
    }

    public String createPortalSession(UUID userId, String returnUrl) {
        initStripe();
        Subscription sub = subscriptionRepository.findByUser(userId)
                .orElseThrow(() -> new BusinessException("No active subscription found"));
        if (sub.stripeCustomerId == null || sub.stripeCustomerId.isBlank()) {
            throw new BusinessException("No Stripe customer associated with this subscription");
        }
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(sub.stripeCustomerId)
                    .setReturnUrl(returnUrl)
                    .build();
            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params);
            return session.getUrl();
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to create portal session: " + e.getMessage());
        }
    }

    @Transactional
    public Subscription upgradeSubscription(UUID userId, String newPlan) {
        initStripe();
        Subscription sub = subscriptionRepository.findByUser(userId)
                .orElseThrow(() -> new BusinessException("No active subscription found"));
        if (sub.stripeSubscriptionId == null || sub.stripeSubscriptionId.isBlank()) {
            throw new BusinessException("No Stripe subscription to upgrade");
        }
        String newPriceId = resolvePriceId(newPlan, "monthly");
        if (newPriceId == null || newPriceId.isBlank()) {
            throw new BusinessException("Stripe price not configured for plan: " + newPlan);
        }
        try {
            com.stripe.model.Subscription stripeSub =
                    com.stripe.model.Subscription.retrieve(sub.stripeSubscriptionId);
            String itemId = stripeSub.getItems().getData().get(0).getId();
            com.stripe.param.SubscriptionUpdateParams params =
                    com.stripe.param.SubscriptionUpdateParams.builder()
                            .addItem(
                                    com.stripe.param.SubscriptionUpdateParams.Item.builder()
                                            .setId(itemId)
                                            .setPrice(newPriceId)
                                            .build()
                            )
                            .setProrationBehavior(
                                    com.stripe.param.SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE
                            )
                            .putMetadata("plan", newPlan.toUpperCase())
                            .putMetadata("userId", userId.toString())
                            .build();
            com.stripe.model.Subscription updated = stripeSub.update(params);
            sub.planName = newPlan.toUpperCase();
            sub.stripePriceId = newPriceId;
            sub.cancelAtPeriodEnd = false;
            sub.currentPeriodStart = java.time.Instant.ofEpochSecond(updated.getCurrentPeriodStart());
            sub.currentPeriodEnd = java.time.Instant.ofEpochSecond(updated.getCurrentPeriodEnd());
            sub.updatedAt = java.time.Instant.now();
            syncProfilePlan(userId, newPlan.toUpperCase());
            return sub;
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to upgrade subscription: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelSubscription(UUID userId) {
        // Find by user regardless of planName (include all active, even if somehow FREE slips through)
        Subscription sub = subscriptionRepository
                .find("user.id = ?1 AND status = 'active' AND planName != 'FREE'", userId)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("No active subscription found"));

        // If no Stripe subscription ID, just mark locally as cancelled
        if (sub.stripeSubscriptionId == null || sub.stripeSubscriptionId.isBlank()) {
            sub.status = "cancelled";
            sub.updatedAt = java.time.Instant.now();
            syncProfilePlan(userId, "FREE");
            return;
        }

        initStripe();
        try {
            com.stripe.model.Subscription stripeSub =
                    com.stripe.model.Subscription.retrieve(sub.stripeSubscriptionId);
            // Schedule cancellation at period end — vendor keeps current tier until billing period expires
            com.stripe.param.SubscriptionUpdateParams params =
                    com.stripe.param.SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(true)
                            .build();
            stripeSub.update(params);
            // Keep status active; only mark that it will cancel at period end
            sub.cancelAtPeriodEnd = true;
            sub.updatedAt = java.time.Instant.now();
            // Do NOT downgrade the tier here — syncProfilePlan("FREE") will be triggered
            // by the customer.subscription.deleted webhook when the period actually ends
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to cancel subscription: " + e.getMessage());
        }
    }

    @Transactional
    public void syncFromCheckoutSession(UUID userId, String sessionId) {
        initStripe();
        try {
            Session session = Session.retrieve(sessionId);
            String stripeSubscriptionId = session.getSubscription();
            if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
                // One-time payment — no subscription object; nothing to sync here
                return;
            }
            com.stripe.model.Subscription stripeSub =
                    com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            String plan = stripeSub.getMetadata().get("plan");
            if (plan == null || plan.isBlank()) {
                plan = resolvePlanFromPriceId(
                        stripeSub.getItems().getData().get(0).getPrice().getId());
            }
            handleSubscriptionCreated(
                    stripeSub.getId(),
                    stripeSub.getCustomer(),
                    stripeSub.getItems().getData().get(0).getPrice().getId(),
                    plan,
                    stripeSub.getStatus(),
                    userId,
                    stripeSub.getCurrentPeriodStart(),
                    stripeSub.getCurrentPeriodEnd()
            );
            syncProfilePlan(userId, plan);
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to sync session: " + e.getMessage());
        }
    }

    /**
     * Best-effort background sync: looks up the user's email in Stripe and syncs any
     * active paid subscription found there into the local DB.
     * Called on subscription page load when DB shows no paid plan, to recover from
     * missed webhooks or redirects (e.g. user navigated away during payment).
     */
    @Transactional
    public void syncFromStripe(UUID userId) {
        // Skip if the user already has an active paid subscription in the DB
        if (subscriptionRepository.findByUser(userId).isPresent()) return;

        initStripe();
        try {
            User user = userRepository.findById(userId);
            if (user == null) return;

            CustomerSearchParams searchParams = CustomerSearchParams.builder()
                    .setQuery("email:'" + user.email + "'")
                    .build();
            CustomerSearchResult customers = Customer.search(searchParams);
            if (customers.getData() == null || customers.getData().isEmpty()) return;

            String customerId = customers.getData().get(0).getId();

            SubscriptionListParams listParams = SubscriptionListParams.builder()
                    .setCustomer(customerId)
                    .setStatus(SubscriptionListParams.Status.ACTIVE)
                    .build();
            SubscriptionCollection subs = com.stripe.model.Subscription.list(listParams);
            if (subs.getData() == null || subs.getData().isEmpty()) return;

            com.stripe.model.Subscription stripeSub = subs.getData().get(0);
            String plan = stripeSub.getMetadata().get("plan");
            if (plan == null || plan.isBlank()) {
                plan = resolvePlanFromPriceId(stripeSub.getItems().getData().get(0).getPrice().getId());
            }
            if (plan == null || plan.isBlank()) return;

            handleSubscriptionCreated(
                    stripeSub.getId(),
                    customerId,
                    stripeSub.getItems().getData().get(0).getPrice().getId(),
                    plan,
                    stripeSub.getStatus(),
                    userId,
                    stripeSub.getCurrentPeriodStart(),
                    stripeSub.getCurrentPeriodEnd()
            );
            syncProfilePlan(userId, plan);
        } catch (com.stripe.exception.StripeException e) {
            // Silently ignore — this is best-effort background sync
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new BusinessException("Stripe webhook secret not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            throw new BusinessException("Invalid Stripe webhook signature");
        }

        switch (event.getType()) {
            case "customer.subscription.created" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    com.stripe.model.Subscription s = (com.stripe.model.Subscription) obj;
                    String userId = s.getMetadata().get("userId");
                    String plan = s.getMetadata().get("plan");
                    // Fallback: resolve plan from price ID if metadata missing
                    if (plan == null || plan.isBlank()) {
                        plan = resolvePlanFromPriceId(s.getItems().getData().get(0).getPrice().getId());
                    }
                    if (userId != null) {
                        handleSubscriptionCreated(
                                s.getId(), s.getCustomer(),
                                s.getItems().getData().get(0).getPrice().getId(),
                                plan,
                                s.getStatus(),
                                UUID.fromString(userId),
                                s.getCurrentPeriodStart(),
                                s.getCurrentPeriodEnd()
                        );
                        syncProfilePlan(UUID.fromString(userId), plan);
                    }
                });
            }
            case "customer.subscription.updated" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    com.stripe.model.Subscription s = (com.stripe.model.Subscription) obj;
                    handleSubscriptionUpdated(s.getId(), s.getStatus(), Boolean.TRUE.equals(s.getCancelAtPeriodEnd()));
                    String userId = s.getMetadata().get("userId");
                    String plan = s.getMetadata().get("plan");
                    if (plan == null || plan.isBlank()) {
                        plan = resolvePlanFromPriceId(s.getItems().getData().get(0).getPrice().getId());
                    }
                    if (userId != null && "active".equals(s.getStatus())) {
                        syncProfilePlan(UUID.fromString(userId), plan);
                    }
                });
            }
            case "customer.subscription.deleted" -> {
                event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                    com.stripe.model.Subscription s = (com.stripe.model.Subscription) obj;
                    handleSubscriptionUpdated(s.getId(), "cancelled", false);
                    String userId = s.getMetadata().get("userId");
                    if (userId != null) {
                        syncProfilePlan(UUID.fromString(userId), "FREE");
                    }
                });
            }
            default -> { /* unhandled event type — ignore */ }
        }
    }

    @Transactional
    void syncProfilePlan(UUID userId, String plan) {
        if (plan == null) return;
        User user = userRepository.findById(userId);
        if (user == null) return;
        if (user.role == UserRole.VENDOR) {
            vendorRepository.findByUserId(userId).ifPresent(v -> {
                try { v.tier = VendorTier.valueOf(plan.toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            });
        } else if (user.role == UserRole.COUPLE) {
            coupleRepository.findByUserId(userId).ifPresent(c -> {
                try { c.plan = CouplePlan.valueOf(plan.toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            });
        }
    }

    @Transactional
    public void handleSubscriptionCreated(String stripeSubscriptionId, String customerId,
                                           String priceId, String planName,
                                           String status, UUID userId,
                                           Long periodStart, Long periodEnd) {
        // Cancel any existing FREE / stale active subscription for this user
        subscriptionRepository.find("user.id = ?1 AND status = 'active'", userId)
                .stream().forEach(existing -> existing.status = "cancelled");

        Subscription sub = new Subscription();
        sub.user = userRepository.findById(userId);
        sub.stripeSubscriptionId = stripeSubscriptionId;
        sub.stripeCustomerId = customerId;
        sub.stripePriceId = priceId;
        sub.planName = planName;
        sub.status = status;
        sub.currentPeriodStart = java.time.Instant.ofEpochSecond(periodStart);
        sub.currentPeriodEnd = java.time.Instant.ofEpochSecond(periodEnd);
        subscriptionRepository.persist(sub);
    }

    @Transactional
    public void handleSubscriptionUpdated(String stripeSubscriptionId, String status, boolean cancelAtEnd) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(sub -> {
                    sub.status = status;
                    sub.cancelAtPeriodEnd = cancelAtEnd;
                    sub.updatedAt = java.time.Instant.now();
                });
    }
}

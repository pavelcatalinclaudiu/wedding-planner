package ro.eternelle.subscription;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionService {

    @ConfigProperty(name = "stripe.secret.key")
    Optional<String> stripeSecretKey;

    @Inject SubscriptionRepository subscriptionRepository;
    @Inject UserRepository userRepository;

    public Optional<Subscription> getActiveSubscription(UUID userId) {
        return subscriptionRepository.findByUser(userId);
    }

    public String createCheckoutSession(UUID userId, String priceId, String successUrl, String cancelUrl) {
        try {
            Stripe.apiKey = stripeSecretKey.orElseThrow(() -> new BusinessException("Stripe is not configured"));
            User user = userRepository.findById(userId);
            if (user == null) throw new BusinessException("User not found");

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(user.email)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("userId", userId.toString())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (com.stripe.exception.StripeException e) {
            throw new BusinessException("Failed to create checkout session: " + e.getMessage());
        }
    }

    @Transactional
    public void handleSubscriptionCreated(String stripeSubscriptionId, String customerId,
                                           String priceId, String planName,
                                           String status, UUID userId,
                                           Long periodStart, Long periodEnd) {
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

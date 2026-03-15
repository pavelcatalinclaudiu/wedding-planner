package ro.eternelle.subscription;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionRepository implements PanacheRepositoryBase<Subscription, UUID> {

    public Optional<Subscription> findByUser(UUID userId) {
        return find("user.id = ?1 AND status = 'active' AND planName != 'FREE'", userId).firstResultOptional();
    }

    public Optional<Subscription> findByStripeSubscriptionId(String stripeId) {
        return find("stripeSubscriptionId", stripeId).firstResultOptional();
    }
}

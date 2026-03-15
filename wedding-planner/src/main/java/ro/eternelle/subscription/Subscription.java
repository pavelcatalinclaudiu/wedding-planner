package ro.eternelle.subscription;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "stripe_subscription_id", unique = true)
    public String stripeSubscriptionId;

    @Column(name = "stripe_customer_id")
    public String stripeCustomerId;

    @Column(name = "stripe_price_id")
    public String stripePriceId;

    @JsonProperty("plan")
    @Column(name = "plan_name", nullable = false)
    public String planName;

    @Column(nullable = false)
    public String status;  // active, past_due, canceled, trialing

    @Column(name = "current_period_start")
    public Instant currentPeriodStart;

    @Column(name = "current_period_end")
    public Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end")
    public boolean cancelAtPeriodEnd = false;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();
}

package ro.eternelle.couple;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "couple_profiles")
public class CoupleProfile extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "partner_1_name")
    public String partner1Name;

    @Column(name = "partner_2_name")
    public String partner2Name;

    @Column(name = "wedding_date")
    public LocalDate weddingDate;

    @Column(name = "wedding_location")
    public String weddingLocation;

    @Column(name = "guest_count")
    @JsonProperty("estimatedGuestCount")
    public int guestCount = 0;

    @Column(name = "total_budget", precision = 10, scale = 2)
    public BigDecimal totalBudget = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    public CouplePlan plan = CouplePlan.FREE;

    @Column(name = "website_subdomain", unique = true)
    @JsonProperty("subdomain")
    public String websiteSubdomain;

    @Column(name = "profile_picture")
    public String profilePicture;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

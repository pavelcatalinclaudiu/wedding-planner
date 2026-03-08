package ro.eternelle.offer;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.conversation.Conversation;
import ro.eternelle.lead.Lead;
import ro.eternelle.vendor.VendorProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    public Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @Column(name = "package_details", columnDefinition = "TEXT")
    public String packageDetails;

    @Column(precision = 12, scale = 2, nullable = false)
    public BigDecimal price;

    @Column(name = "expires_at")
    public LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    public OfferStatus status = OfferStatus.PENDING;

    @Column(name = "viewed_at")
    public Instant viewedAt;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

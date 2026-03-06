package ro.eternelle.lead;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.vendor.VendorProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leads")
public class Lead extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @Column(name = "event_date")
    public LocalDate eventDate;

    @Column(name = "budget", precision = 12, scale = 2)
    public BigDecimal budget;

    @Column(name = "message", columnDefinition = "TEXT")
    public String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public LeadStatus status = LeadStatus.NEW;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();
}

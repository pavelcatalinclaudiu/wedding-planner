package ro.eternelle.review;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.booking.Booking;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.vendor.VendorProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id"}))
public class Review extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    public Booking booking;

    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @Column(nullable = false, precision = 2, scale = 1)
    public BigDecimal rating;   // 1.0 – 5.0

    @Column(columnDefinition = "TEXT")
    public String comment;

    @Column(name = "vendor_reply", columnDefinition = "TEXT")
    public String vendorReply;

    @Column(name = "is_public")
    public boolean isPublic = true;

    /**
     * Moderation state: PENDING (awaiting admin approval), APPROVED (visible on vendor page),
     * REJECTED (hidden from public).
     */
    @Column(name = "status", nullable = false, length = 20)
    public String status = "PENDING";

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

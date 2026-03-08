package ro.eternelle.booking;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.lead.Lead;
import ro.eternelle.offer.Offer;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @OneToOne(optional = true)
    @JoinColumn(name = "lead_id")
    public Lead lead;

    @OneToOne(optional = true)
    @JoinColumn(name = "offer_id")
    public Offer offer;

    @Column(name = "wedding_date")
    public LocalDate weddingDate;

    @Column(name = "total_price", precision = 12, scale = 2, nullable = false)
    public BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "deposit_paid", precision = 12, scale = 2)
    public BigDecimal depositPaid = BigDecimal.ZERO;

    @Column(name = "deposit_paid_at")
    public Instant depositPaidAt;

    @Column(name = "balance_due_at")
    public LocalDate balanceDueAt;

    @Column(name = "contract_url")
    public String contractUrl;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "status", length = 30)
    @Enumerated(EnumType.STRING)
    public BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "proposed_date")
    public LocalDate proposedDate;

    @Column(name = "proposed_note", length = 500)
    public String proposedNote;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

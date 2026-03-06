package ro.eternelle.budget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_items")
public class BudgetItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    @Column(nullable = false)
    public String category;

    @Column(nullable = false)
    public String name;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    public BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    public BigDecimal actualCost;

    @Column(name = "is_paid")
    public boolean isPaid = false;

    @Column(name = "vendor_name")
    public String vendorName;

    public String notes;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

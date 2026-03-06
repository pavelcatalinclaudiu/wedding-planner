package ro.eternelle.vendor;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vendor_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "date"}))
public class VendorAvailability extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @Column(nullable = false)
    public LocalDate date;

    @Column
    public String reason;
}

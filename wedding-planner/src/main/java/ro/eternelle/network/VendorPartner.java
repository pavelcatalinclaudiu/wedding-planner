package ro.eternelle.network;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.vendor.VendorProfile;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_partners",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vendor_id", "partner_id"}))
public class VendorPartner extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    public VendorProfile partner;  // null when partner is not on the platform

    @Column(name = "partner_name")
    public String partnerName;  // custom name for off-platform partners

    @Column(name = "partnership_note")
    public String partnershipNote;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

package ro.eternelle.vendor;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_photos")
public class VendorPhoto extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    @JsonBackReference
    public VendorProfile vendor;

    @Transient
    @JsonProperty("vendorId")
    public UUID getVendorId() {
        return vendor != null ? vendor.id : null;
    }

    @Column(name = "url", nullable = false)
    public String url;

    @Column(name = "caption")
    public String caption;

    @Column(name = "is_cover")
    public boolean isCover = false;

    @Column(name = "sort_order")
    @JsonProperty("order")
    public int sortOrder = 0;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

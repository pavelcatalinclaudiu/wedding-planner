package ro.eternelle.vendor;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_views")
public class ProfileView extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    public VendorProfile vendor;

    @ManyToOne
    @JoinColumn(name = "viewer_id")
    public User viewer;   // null for anonymous visitors

    @Column(name = "ip_hash")
    public String ipHash;

    @Column(name = "viewed_at")
    public Instant viewedAt = Instant.now();
}

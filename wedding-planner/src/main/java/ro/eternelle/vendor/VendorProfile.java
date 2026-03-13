package ro.eternelle.vendor;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendor_profiles")
public class VendorProfile extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "business_name", nullable = false)
    public String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VendorCategory category;

    @Column(nullable = false)
    public String city;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "base_price", precision = 10, scale = 2)
    public BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    public VendorTier tier = VendorTier.FREE;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @JsonProperty("averageRating")
    public BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    public int reviewCount = 0;

    @Column(name = "profile_views")
    public int profileViews = 0;

    @Column(name = "is_verified")
    public boolean isVerified = false;

    @Column(name = "is_active")
    @JsonProperty("active")
    public boolean isActive = true;

    @Column(name = "profile_picture")
    public String profilePicture;

    @Column(name = "cover_photo")
    public String coverPhoto;

    @Column(name = "website")
    public String website;

    @Column(name = "instagram")
    public String instagram;

    @Column(name = "facebook")
    public String facebook;

    @OneToMany(mappedBy = "vendor", fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @JsonManagedReference
    public List<VendorPhoto> photos = new ArrayList<>();

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

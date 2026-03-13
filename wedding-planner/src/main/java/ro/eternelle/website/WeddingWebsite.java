package ro.eternelle.website;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wedding_websites")
public class WeddingWebsite extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    @Column(name = "hero_message", length = 500)
    public String heroMessage;

    @Column(name = "cover_photo_url", length = 1000)
    public String coverPhotoUrl;

    @Column(columnDefinition = "TEXT")
    public String story;

    @Column(name = "ceremony_date", length = 100)
    public String ceremonyDate;

    @Column(name = "ceremony_location", length = 500)
    public String ceremonyLocation;

    @Column(name = "reception_date", length = 100)
    public String receptionDate;

    @Column(name = "reception_location", length = 500)
    public String receptionLocation;

    @Column(name = "is_published", nullable = false)
    public boolean published = false;

    @Column(name = "rsvps_submitted", nullable = false)
    public int rsvpsSubmitted = 0;

    @Column(name = "visitor_count", nullable = false)
    public int visitorCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}

package ro.eternelle.guest;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guests")
public class Guest extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    /** For +1 guests: the primary guest who brought them. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    public Guest invitedBy;

    @Column(name = "first_name", nullable = false)
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    public String email;

    public String phone;

    /** BRIDE | GROOM | BOTH */
    @Column(name = "side")
    public String side = "BOTH";

    /** FAMILY | FRIENDS | COLLEAGUES | OTHER */
    @Column(name = "guest_group")
    public String guestGroup = "OTHER";

    /** PENDING | CONFIRMED | DECLINED | MAYBE */
    @Column(name = "rsvp_status")
    public String rsvpStatus = "PENDING";

    @Column(name = "plus_one")
    public boolean plusOne = false;

    @Column(name = "plus_one_name")
    public String plusOneName;

    /** NONE | VEGAN | VEGETARIAN | GLUTEN_FREE | HALAL | KOSHER | OTHER */
    @Column(name = "dietary")
    public String dietary = "NONE";

    @Column(name = "dietary_notes")
    public String dietaryNotes;

    @Column(name = "table_assignment")
    public String tableAssignment;

    @Column(name = "song_request")
    public String songRequest;

    @Column(name = "notes")
    public String notes;

    @Column(name = "is_child_guest")
    public boolean isChildGuest = false;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    public Instant updatedAt;
}

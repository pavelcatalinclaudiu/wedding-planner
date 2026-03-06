package ro.eternelle.notification;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(nullable = false)
    public String type;   // new_enquiry, new_message, quote_sent, deal_sealed, etc.

    @Column(nullable = false)
    public String title;

    public String body;

    @Column(name = "entity_id")
    public String entityId;

    @Column(name = "read_flag")
    public boolean read = false;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

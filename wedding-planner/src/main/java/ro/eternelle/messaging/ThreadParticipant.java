package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thread_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"thread_id", "user_id"}))
public class ThreadParticipant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    public Thread thread;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "joined_at")
    public Instant joinedAt = Instant.now();
}

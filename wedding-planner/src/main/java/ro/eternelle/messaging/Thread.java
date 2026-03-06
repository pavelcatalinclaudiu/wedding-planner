package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.lead.Lead;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threads")
public class Thread extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @OneToOne
    @JoinColumn(name = "lead_id")
    public Lead lead;

    /** Set only for GROUP threads; identifies the couple that owns this chat. */
    @ManyToOne
    @JoinColumn(name = "couple_id")
    public CoupleProfile couple;

    @Enumerated(EnumType.STRING)
    @Column(name = "thread_type", nullable = false)
    public ThreadType threadType = ThreadType.DIRECT;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    @Column(name = "last_message_at")
    public Instant lastMessageAt = Instant.now();
}

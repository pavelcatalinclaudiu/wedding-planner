package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.converter.UUIDListConverter;
import ro.eternelle.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    public Thread thread;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    public User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String content;

    @Enumerated(EnumType.STRING)
    public MessageType type = MessageType.TEXT;

    @Column(name = "read_by", columnDefinition = "jsonb")
    @Convert(converter = UUIDListConverter.class)
    public List<UUID> readBy = new ArrayList<>();

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

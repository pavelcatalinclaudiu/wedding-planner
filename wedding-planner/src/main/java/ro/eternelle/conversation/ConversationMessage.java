package ro.eternelle.conversation;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    public Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    public User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false)
    public SenderRole senderRole;

    @Column(name = "content", columnDefinition = "TEXT")
    public String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public ConversationMessageType type = ConversationMessageType.TEXT;

    @Column(name = "sent_at")
    public Instant sentAt = Instant.now();

    public enum SenderRole { COUPLE, VENDOR }
}

package ro.eternelle.videocall;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.lead.Lead;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_calls")
public class VideoCall extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Column(name = "room_url")
    public String roomUrl;

    @Column(name = "room_name")
    public String roomName;

    @Column(name = "scheduled_at")
    public Instant scheduledAt;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "ended_at")
    public Instant endedAt;

    @Enumerated(EnumType.STRING)
    public VideoCallStatus status = VideoCallStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_call_action")
    public PostCallAction postCallAction;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();

    /** "COUPLE" or "VENDOR" – whoever proposed or last rescheduled this call. */
    @Column(name = "proposed_by")
    public String proposedBy;
}

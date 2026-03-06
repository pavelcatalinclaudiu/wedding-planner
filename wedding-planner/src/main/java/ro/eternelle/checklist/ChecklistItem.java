package ro.eternelle.checklist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    @JsonIgnore
    public CoupleProfile couple;

    /** DB column is "title"; JSON field is naturally "label" — no @JsonProperty needed. */
    @Column(name = "title", nullable = false)
    public String label;

    @Column(name = "description", columnDefinition = "TEXT")
    public String notes;

    public String category;

    @Column(name = "time_period")
    public String timePeriod;

    @Column(name = "due_date")
    public LocalDate dueDate;

    /** DB column is "completed"; JSON field is naturally "done" — no @JsonProperty needed. */
    @Column(name = "completed")
    public boolean done = false;

    @Column(name = "completed_at")
    @JsonIgnore
    public Instant completedAt;

    @Column(name = "sort_order")
    @JsonIgnore
    public int sortOrder = 0;

    @Column(name = "is_default")
    @JsonProperty("auto")
    public boolean isAuto = false;

    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

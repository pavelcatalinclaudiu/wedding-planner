package ro.eternelle.document;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ro.eternelle.couple.CoupleProfile;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "couple_documents")
public class CoupleDocument extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    public CoupleProfile couple;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String filename;

    public long size;

    @Column(name = "uploaded_at")
    public Instant uploadedAt = Instant.now();
}

package ro.eternelle.platform;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "page_visits")
public class PageVisit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public UUID id;

    @Column(name = "ip_hash")
    public String ipHash;

    @Column(name = "visited_at", nullable = false)
    public Instant visitedAt = Instant.now();
}

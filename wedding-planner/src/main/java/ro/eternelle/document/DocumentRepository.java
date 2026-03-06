package ro.eternelle.document;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentRepository implements PanacheRepositoryBase<CoupleDocument, UUID> {

    public List<CoupleDocument> findByCouple(UUID coupleId) {
        return list("couple.id = ?1 ORDER BY uploadedAt DESC", coupleId);
    }
}

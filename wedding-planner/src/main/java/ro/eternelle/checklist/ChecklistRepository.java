package ro.eternelle.checklist;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChecklistRepository implements PanacheRepositoryBase<ChecklistItem, UUID> {

    public List<ChecklistItem> findByCouple(UUID coupleId) {
        return find("couple.id = ?1 ORDER BY sortOrder, createdAt", coupleId).list();
    }
}

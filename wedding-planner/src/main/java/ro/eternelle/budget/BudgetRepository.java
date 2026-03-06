package ro.eternelle.budget;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BudgetRepository implements PanacheRepositoryBase<BudgetItem, UUID> {

    public List<BudgetItem> findByCouple(UUID coupleId) {
        return find("couple.id = ?1 ORDER BY category, name", coupleId).list();
    }
}

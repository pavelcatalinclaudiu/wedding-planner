package ro.eternelle.checklist;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChecklistService {

    @Inject ChecklistRepository checklistRepository;
    @Inject CoupleRepository coupleRepository;

    public List<ChecklistItem> getByCouple(UUID coupleUserId) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
        return checklistRepository.findByCouple(couple.id);
    }

    @Transactional
    public ChecklistItem addItem(UUID coupleUserId, ChecklistItemRequest req) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
        ChecklistItem item = new ChecklistItem();
        item.couple = couple;
        item.label = req.label;
        item.notes = req.notes;
        item.category = req.category;
        item.timePeriod = req.timePeriod;
        item.dueDate = req.dueDate;
        checklistRepository.persist(item);
        return item;
    }

    @Transactional
    public ChecklistItem toggleComplete(UUID itemId, UUID coupleUserId) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
        ChecklistItem item = checklistRepository
                .find("id = ?1 and couple.id = ?2", itemId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Checklist item not found"));
        item.done = !item.done;
        item.completedAt = item.done ? Instant.now() : null;
        return item;
    }

    @Transactional
    public ChecklistItem updateItem(UUID itemId, UUID coupleUserId, ChecklistItemRequest req) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
        ChecklistItem item = checklistRepository
                .find("id = ?1 and couple.id = ?2", itemId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Checklist item not found"));
        if (req.label != null)      item.label      = req.label;
        if (req.notes != null)      item.notes      = req.notes;
        if (req.category != null)   item.category   = req.category;
        if (req.timePeriod != null) item.timePeriod = req.timePeriod;
        if (req.dueDate != null)    item.dueDate    = req.dueDate;
        return item;
    }

    @Transactional
    public void deleteItem(UUID itemId, UUID coupleUserId) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
        ChecklistItem item = checklistRepository
                .find("id = ?1 and couple.id = ?2", itemId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Checklist item not found"));
        checklistRepository.delete(item);
    }
}

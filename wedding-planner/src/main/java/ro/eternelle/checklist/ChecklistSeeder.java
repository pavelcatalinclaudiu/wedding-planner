package ro.eternelle.checklist;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.couple.CoupleProfile;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds default checklist tasks when a couple profile is created.
 */
@ApplicationScoped
public class ChecklistSeeder {

    @Inject
    ChecklistRepository checklistRepository;

    private static final List<String[]> DEFAULT_TASKS = List.of(
            new String[]{"Set your wedding date", "Planning"},
            new String[]{"Set your budget", "Planning"},
            new String[]{"Book your venue", "Venue"},
            new String[]{"Book a photographer", "Vendors"},
            new String[]{"Book a caterer", "Vendors"},
            new String[]{"Send save-the-dates", "Invitations"},
            new String[]{"Book accommodation for guests", "Guests"},
            new String[]{"Order wedding cake", "Vendors"},
            new String[]{"Arrange flowers & decorations", "Decor"},
            new String[]{"Plan the honeymoon", "Travel"},
            new String[]{"Send formal invitations", "Invitations"},
            new String[]{"Confirm guest list & RSVPs", "Guests"},
            new String[]{"Create a seating plan", "Guests"},
            new String[]{"Confirm all vendor bookings", "Vendors"},
            new String[]{"Prepare vows", "Ceremony"},
            new String[]{"Arrange transport for wedding day", "Logistics"},
            new String[]{"Final dress fitting", "Attire"},
            new String[]{"Create wedding day schedule", "Planning"},
            new String[]{"Prepare final payments for vendors", "Finance"},
            new String[]{"Enjoy your wedding day!", "Day-of"}
    );

    @Transactional
    public void seedFor(CoupleProfile couple) {
        for (int i = 0; i < DEFAULT_TASKS.size(); i++) {
            ChecklistItem item = new ChecklistItem();
            item.couple = couple;
            item.label = DEFAULT_TASKS.get(i)[0];
            item.category = DEFAULT_TASKS.get(i)[1];
            item.sortOrder = i;
            item.isAuto = true;
            checklistRepository.persist(item);
        }
    }
}

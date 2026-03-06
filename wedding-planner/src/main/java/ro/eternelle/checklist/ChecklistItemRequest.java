package ro.eternelle.checklist;

import java.time.LocalDate;

/** Request DTO for creating or updating a checklist item. */
public class ChecklistItemRequest {
    public String label;
    public String notes;
    public String category;
    public String timePeriod;
    public LocalDate dueDate;
}

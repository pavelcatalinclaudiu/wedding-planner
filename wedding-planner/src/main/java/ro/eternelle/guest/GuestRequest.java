package ro.eternelle.guest;

/** Inbound payload for create or update guest requests. */
public class GuestRequest {

    public String firstName;
    public String lastName;
    public String email;
    public String phone;

    /** BRIDE | GROOM | BOTH */
    public String side = "BOTH";

    /** FAMILY | FRIENDS | COLLEAGUES | OTHER */
    public String guestGroup = "OTHER";

    /** PENDING | CONFIRMED | DECLINED | MAYBE */
    public String rsvpStatus = "PENDING";

    public boolean plusOne = false;
    public String plusOneName;

    /** NONE | VEGAN | VEGETARIAN | GLUTEN_FREE | HALAL | KOSHER | OTHER */
    public String dietary = "NONE";
    public String dietaryNotes;

    public String tableAssignment;
    public String songRequest;
    public String notes;
    public boolean isChildGuest = false;
}

package ro.eternelle.website;

public class RsvpRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    /** "CONFIRMED" or "DECLINED" */
    public String attending;
    /** dietary preference: NONE, VEGAN, VEGETARIAN, GLUTEN_FREE, HALAL, KOSHER, OTHER */
    public String dietary;
    public String dietaryNotes;
    public boolean plusOne;
    public String plusOneName;
    /** dietary preference for the +1: NONE, VEGAN, VEGETARIAN, GLUTEN_FREE, HALAL, KOSHER, OTHER */
    public String plusOneDietary;
    public String plusOneDietaryNotes;
    public String message;
    public String songRequest;
    /** Optional personal invite token – when present, guest is matched by token first */
    public String inviteToken;
}

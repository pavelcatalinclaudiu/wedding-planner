package ro.eternelle.vendor;

public enum VendorCategory {
    PHOTOGRAPHER("Photographer"),
    VIDEOGRAPHER("Videographer"),
    FLORIST("Florist"),
    CATERER("Caterer"),
    VENUE("Venue"),
    BAND("Live Band"),
    DJ("DJ"),
    CAKE("Cake"),
    MAKEUP_ARTIST("Makeup Artist"),
    HAIR_STYLIST("Hair Stylist"),
    OFFICIANT("Officiant"),
    PLANNER("Wedding Planner"),
    TRANSPORTATION("Transportation"),
    LIGHTING("Lighting"),
    INVITATION_STATIONERY("Invitations & Stationery"),
    JEWELRY("Jewelry"),
    OTHER("Other");

    public final String displayName;

    VendorCategory(String displayName) {
        this.displayName = displayName;
    }
}

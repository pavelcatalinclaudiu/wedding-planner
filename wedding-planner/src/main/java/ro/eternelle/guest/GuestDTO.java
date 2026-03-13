package ro.eternelle.guest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GuestDTO {

    public UUID id;
    public UUID coupleId;
    public UUID invitedById;

    public String firstName;
    public String lastName;
    public String fullName;

    public String email;
    public String phone;
    public String side;
    public String guestGroup;
    public String rsvpStatus;

    public boolean plusOne;
    public String plusOneName;

    public String dietary;
    public String dietaryNotes;

    public String tableAssignment;
    public String songRequest;
    public String notes;
    public boolean isChildGuest;

    /** UUID token for personal invite link */
    public String inviteToken;
    /**
     * NOT_INVITED – no link generated yet<br>
     * LINK_SENT   – link generated but not yet opened<br>
     * PENDING     – guest opened the link, awaiting response<br>
     * ACCEPTED    – RSVP confirmed<br>
     * DECLINED    – RSVP declined<br>
     * MAYBE       – RSVP maybe
     */
    public String inviteStatus;

    public Instant createdAt;
    public Instant updatedAt;

    // ── factory ────────────────────────────────────────────────────────────

    public static GuestDTO from(Guest g) {
        GuestDTO dto = new GuestDTO();
        dto.id           = g.id;
        dto.coupleId     = g.couple != null ? g.couple.id : null;
        dto.invitedById  = g.invitedBy != null ? g.invitedBy.id : null;
        dto.firstName    = g.firstName;
        dto.lastName     = g.lastName;
        dto.fullName     = (g.firstName != null ? g.firstName : "")
                         + (g.lastName  != null ? " " + g.lastName : "");
        dto.email        = g.email;
        dto.phone        = g.phone;
        dto.side         = g.side;
        dto.guestGroup   = g.guestGroup;
        dto.rsvpStatus   = g.rsvpStatus;
        dto.plusOne      = g.plusOne;
        dto.plusOneName  = g.plusOneName;
        dto.dietary      = g.dietary;
        dto.dietaryNotes = g.dietaryNotes;
        dto.tableAssignment = g.tableAssignment;
        dto.songRequest  = g.songRequest;
        dto.notes        = g.notes;
        dto.isChildGuest = g.isChildGuest;
        dto.inviteToken  = g.inviteToken;
        dto.inviteStatus = computeInviteStatus(g);
        dto.createdAt    = g.createdAt;
        dto.updatedAt    = g.updatedAt;
        return dto;
    }

    private static String computeInviteStatus(Guest g) {
        if ("CONFIRMED".equals(g.rsvpStatus)) return "ACCEPTED";
        if ("DECLINED" .equals(g.rsvpStatus)) return "DECLINED";
        if ("MAYBE"    .equals(g.rsvpStatus)) return "MAYBE";
        // rsvpStatus is PENDING — check invite progress
        if (g.inviteToken == null)        return "NOT_INVITED";
        if (g.inviteOpenedAt == null)     return "LINK_SENT";
        return "PENDING";
    }

    // ── stats inner class ───────────────────────────────────────────────────

    public static class Stats {
        public long total;
        public long confirmed;
        public long declined;
        public long pending;
        public long maybe;
        public int  estimatedCapacity;
        public boolean overCapacity;
        public Map<String, Long> byDietary;
        public Map<String, Long> bySide;
        public Map<String, Long> byGroup;
    }
}

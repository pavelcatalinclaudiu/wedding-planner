package ro.eternelle.guest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.website.RsvpRequest;
import ro.eternelle.website.WeddingWebsiteService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GuestService {

    @Inject GuestRepository guestRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject NotificationService notificationService;
    @Inject WeddingWebsiteService websiteService;

    // ── Read ───────────────────────────────────────────────────────────────

    public List<GuestDTO> getGuests(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        return guestRepository.findByCouple(couple.id)
                .stream().map(GuestDTO::from).collect(Collectors.toList());
    }

    // ── Create ─────────────────────────────────────────────────────────────

    @Transactional
    public GuestDTO addGuest(UUID coupleUserId, GuestRequest req) {
        CoupleProfile couple = requireCouple(coupleUserId);
        Guest g = applyRequest(new Guest(), req);
        g.couple    = couple;
        g.createdAt = Instant.now();
        g.updatedAt = Instant.now();
        guestRepository.persist(g);
        // auto-create +1 companion
        if (req.plusOne && req.plusOneName != null && !req.plusOneName.isBlank()) {
            Guest companion = new Guest();
            companion.couple     = couple;
            companion.firstName  = req.plusOneName;
            companion.lastName   = "";
            companion.side       = g.side;
            companion.guestGroup = g.guestGroup;
            companion.tableAssignment = g.tableAssignment;
            companion.invitedBy  = g;
            companion.createdAt  = Instant.now();
            companion.updatedAt  = Instant.now();
            guestRepository.persist(companion);
        }
        return GuestDTO.from(g);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    @Transactional
    public GuestDTO updateGuest(UUID guestId, UUID coupleUserId, GuestRequest req) {
        CoupleProfile couple = requireCouple(coupleUserId);
        Guest g = guestRepository.find("id = ?1 and couple.id = ?2", guestId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Guest not found"));
        applyRequest(g, req);
        g.updatedAt = Instant.now();
        return GuestDTO.from(g);
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteGuest(UUID guestId, UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        Guest g = guestRepository.find("id = ?1 and couple.id = ?2", guestId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Guest not found"));
        guestRepository.delete(g);
    }

    // ── Stats ──────────────────────────────────────────────────────────────

    public GuestDTO.Stats getStats(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        UUID cid = couple.id;
        List<Guest> all = guestRepository.findByCouple(cid);

        GuestDTO.Stats s = new GuestDTO.Stats();
        s.total     = all.size();
        s.confirmed = all.stream().filter(g -> "CONFIRMED".equals(g.rsvpStatus)).count();
        s.declined  = all.stream().filter(g -> "DECLINED" .equals(g.rsvpStatus)).count();
        s.pending   = all.stream().filter(g -> "PENDING"  .equals(g.rsvpStatus)).count();
        s.maybe     = all.stream().filter(g -> "MAYBE"    .equals(g.rsvpStatus)).count();

        s.estimatedCapacity = couple.guestCount;
        s.overCapacity = s.estimatedCapacity > 0 && s.confirmed > s.estimatedCapacity;

        s.byDietary = all.stream().collect(
                Collectors.groupingBy(
                    g -> g.dietary != null ? g.dietary : "NONE",
                    Collectors.counting()));

        s.bySide = all.stream().collect(
                Collectors.groupingBy(
                    g -> g.side != null ? g.side : "BOTH",
                    Collectors.counting()));

        s.byGroup = all.stream().collect(
                Collectors.groupingBy(
                    g -> g.guestGroup != null ? g.guestGroup : "OTHER",
                    Collectors.counting()));
        return s;
    }

    // ── Song Requests ──────────────────────────────────────────────────────

    public List<String> getSongRequests(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        return guestRepository.findSongRequests(couple.id);
    }

    // ── Public RSVP ────────────────────────────────────────────────────────

    @Transactional
    public void submitPublicRsvp(String subdomain, RsvpRequest req) {
        if (req.firstName == null || req.firstName.isBlank())
            throw new BusinessException("First name is required");
        if (req.attending == null)
            throw new BusinessException("Attendance answer is required");

        CoupleProfile couple = coupleRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new BusinessException("Wedding website not found"));

        // Upsert: match by token first, then email, then create new
        Guest g = null;
        if (req.inviteToken != null && !req.inviteToken.isBlank()) {
            g = guestRepository.findByInviteToken(req.inviteToken).orElse(null);
        }
        if (g == null && req.email != null && !req.email.isBlank()) {
            g = guestRepository.find(
                    "couple.id = ?1 and lower(email) = lower(?2)",
                    couple.id, req.email.trim()
            ).firstResult();
        }

        if (g == null) {
            g = new Guest();
            g.couple    = couple;
            g.createdAt = Instant.now();
        }

        g.firstName    = req.firstName.trim();
        g.lastName     = req.lastName  != null ? req.lastName.trim() : "";
        g.email        = req.email     != null ? req.email.trim()    : null;
        g.phone        = req.phone;
        g.rsvpStatus   = "CONFIRMED".equalsIgnoreCase(req.attending) ? "CONFIRMED" : "DECLINED";
        g.dietary      = req.dietary   != null ? req.dietary         : "NONE";
        g.dietaryNotes = req.dietaryNotes;
        g.plusOne      = req.plusOne;
        g.plusOneName  = req.plusOneName;
        g.songRequest  = req.songRequest;
        g.notes        = req.message;
        g.updatedAt    = Instant.now();

        if (g.id == null) {
            guestRepository.persist(g);
        }

        // Auto-create +1 companion
        if (req.plusOne && req.plusOneName != null && !req.plusOneName.isBlank()
                && "CONFIRMED".equalsIgnoreCase(req.attending)) {
            Guest companion = guestRepository.find(
                    "invitedBy.id = ?1", g.id).firstResultOptional().orElse(null);
            if (companion == null) {
                companion = new Guest();
                companion.couple    = couple;
                companion.invitedBy = g;
                companion.side      = g.side;
                companion.createdAt = Instant.now();
            }
            companion.firstName    = req.plusOneName;
            companion.lastName     = "";
            companion.rsvpStatus   = "CONFIRMED";
            companion.dietary      = req.plusOneDietary    != null ? req.plusOneDietary    : "NONE";
            companion.dietaryNotes = req.plusOneDietaryNotes;
            companion.updatedAt    = Instant.now();
            if (companion.id == null) {
                guestRepository.persist(companion);
            }
        }

        // Increment RSVP counter on website
        websiteService.incrementRsvpCount(couple);

        // Notify couple
        String guestName = g.firstName + (g.lastName != null && !g.lastName.isBlank()
                ? " " + g.lastName : "");
        String status = "CONFIRMED".equals(g.rsvpStatus) ? "is attending ✓" : "declined ✗";
        if (couple.user != null) {
            notificationService.create(
                    couple.user.id,
                    "RSVP_RECEIVED",
                    "New RSVP",
                    guestName + " " + status,
                    g.id
            );
        }
    }

    // ── Invite link ────────────────────────────────────────────────

    /** Generate (or re-use) an invite token for a guest and return it. */
    @Transactional
    public String generateInviteToken(UUID guestId, UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        Guest g = guestRepository.find("id = ?1 and couple.id = ?2", guestId, couple.id)
                .firstResultOptional()
                .orElseThrow(() -> new BusinessException("Guest not found"));
        if (g.inviteToken == null) {
            g.inviteToken = UUID.randomUUID().toString();
            g.updatedAt   = Instant.now();
        }
        return g.inviteToken;
    }

    /**
     * Called when a guest opens the personal invite link.
     * Marks inviteOpenedAt (first open only) and returns guest info for prefilling the RSVP form.
     */
    @Transactional
    public GuestDTO openInviteLink(String token) {
        Guest g = guestRepository.findByInviteToken(token)
                .orElseThrow(() -> new BusinessException("Invalid invite link"));
        if (g.inviteOpenedAt == null) {
            g.inviteOpenedAt = Instant.now();
            g.updatedAt      = Instant.now();
        }
        return GuestDTO.from(g);
    }

    // ── CSV Import ─────────────────────────────────────────────────────────

    @Transactional
    public int importCsv(UUID coupleUserId, String csv) {
        CoupleProfile couple = requireCouple(coupleUserId);
        int count = 0;
        for (String line : csv.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.toLowerCase().startsWith("firstname") || line.toLowerCase().startsWith("name"))
                continue;
            String[] cols = line.split(",", -1);
            Guest g = new Guest();
            g.couple    = couple;
            g.firstName = cols.length > 0 ? cols[0].trim() : "";
            g.lastName  = cols.length > 1 ? cols[1].trim() : "";
            g.email     = cols.length > 2 ? cols[2].trim() : null;
            g.phone     = cols.length > 3 ? cols[3].trim() : null;
            g.createdAt = Instant.now();
            g.updatedAt = Instant.now();
            if (!g.firstName.isEmpty()) {
                guestRepository.persist(g);
                count++;
            }
        }
        return count;
    }

    // ── CSV Export ─────────────────────────────────────────────────────────

    public String exportCsv(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        List<Guest> guests = guestRepository.findByCouple(couple.id);
        StringBuilder sb = new StringBuilder();
        sb.append("FirstName,LastName,Email,Phone,Side,Group,RSVP,PlusOne,PlusOneName,Dietary,Table,SongRequest,Notes\n");
        for (Guest g : guests) {
            sb.append(csv(g.firstName)).append(",")
              .append(csv(g.lastName)).append(",")
              .append(csv(g.email)).append(",")
              .append(csv(g.phone)).append(",")
              .append(csv(g.side)).append(",")
              .append(csv(g.guestGroup)).append(",")
              .append(csv(g.rsvpStatus)).append(",")
              .append(g.plusOne ? "Yes" : "No").append(",")
              .append(csv(g.plusOneName)).append(",")
              .append(csv(g.dietary)).append(",")
              .append(csv(g.tableAssignment)).append(",")
              .append(csv(g.songRequest)).append(",")
              .append(csv(g.notes)).append("\n");
        }
        return sb.toString();
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private CoupleProfile requireCouple(UUID userId) {
        return coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Couple not found"));
    }

    private Guest applyRequest(Guest g, GuestRequest req) {
        if (req.firstName    != null) g.firstName    = req.firstName;
        if (req.lastName     != null) g.lastName     = req.lastName;
        if (req.email        != null) g.email        = req.email;
        if (req.phone        != null) g.phone        = req.phone;
        if (req.side         != null) g.side         = req.side;
        if (req.guestGroup   != null) g.guestGroup   = req.guestGroup;
        if (req.rsvpStatus   != null) g.rsvpStatus   = req.rsvpStatus;
        g.plusOne      = req.plusOne;
        if (req.plusOneName  != null) g.plusOneName  = req.plusOneName;
        if (req.dietary      != null) g.dietary      = req.dietary;
        if (req.dietaryNotes != null) g.dietaryNotes = req.dietaryNotes;
        if (req.tableAssignment != null) g.tableAssignment = req.tableAssignment;
        if (req.songRequest  != null) g.songRequest  = req.songRequest;
        if (req.notes        != null) g.notes        = req.notes;
        g.isChildGuest = req.isChildGuest;
        return g;
    }

    private static String csv(String v) {
        if (v == null || v.isEmpty()) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}

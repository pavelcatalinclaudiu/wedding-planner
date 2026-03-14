package ro.eternelle.website;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import static jakarta.transaction.Transactional.TxType.REQUIRED;
import jakarta.ws.rs.NotFoundException;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;

import java.util.Locale;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class WeddingWebsiteService {

    @Inject WeddingWebsiteRepository websiteRepository;
    @Inject CoupleRepository coupleRepository;

    // ── Get (read-only, no auto-create) ────────────────────────────────────

    @Transactional(REQUIRED)
    public WeddingWebsiteDTO.Response get(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        WeddingWebsite site = websiteRepository.findByCouple(couple.id)
                .orElseThrow(NotFoundException::new);
        return WeddingWebsiteDTO.Response.from(site);
    }

    private WeddingWebsite createNew(CoupleProfile couple) {
        WeddingWebsite site = new WeddingWebsite();
        site.couple = couple;
        if (couple.websiteSubdomain == null || couple.websiteSubdomain.isBlank()) {
            couple.websiteSubdomain = generateUniqueSlug(couple);
        }
        websiteRepository.persist(site);
        return site;
    }

    // ── Update ─────────────────────────────────────────────────────────────

    @Transactional(REQUIRED)
    public WeddingWebsiteDTO.Response update(UUID coupleUserId, WeddingWebsiteDTO.UpdateRequest req) {
        CoupleProfile couple = requireCouple(coupleUserId);

        // Update subdomain on CoupleProfile if changed
        if (req.subdomain != null && !req.subdomain.isBlank()) {
            String slug = toSlug(req.subdomain);
            if (couple.subdomainCustomized) {
                throw new BusinessException("Your website URL has already been customized and cannot be changed again.");
            }
            // Check uniqueness (excluding current couple)
            coupleRepository.findBySubdomain(slug).ifPresent(other -> {
                if (!other.id.equals(couple.id)) {
                    throw new BusinessException("This URL is already taken. Please choose another.");
                }
            });
            couple.websiteSubdomain = slug;
            couple.subdomainCustomized = true;
        }

        WeddingWebsite site = websiteRepository.findByCouple(couple.id)
                .orElseGet(() -> createNew(couple));

        if (req.heroMessage       != null) site.heroMessage       = req.heroMessage;
        if (req.story             != null) site.story             = req.story;
        if (req.ceremonyDate      != null) site.ceremonyDate      = req.ceremonyDate;
        if (req.ceremonyLocation  != null) site.ceremonyLocation  = req.ceremonyLocation;
        if (req.receptionDate     != null) site.receptionDate     = req.receptionDate;
        if (req.receptionLocation != null) site.receptionLocation = req.receptionLocation;
        if (req.published         != null) site.published         = req.published;

        return WeddingWebsiteDTO.Response.from(site);
    }

    // ── Update cover photo ─────────────────────────────────────────────────

    @Transactional
    public WeddingWebsiteDTO.Response updateCoverPhoto(UUID coupleUserId, String photoUrl) {
        CoupleProfile couple = requireCouple(coupleUserId);
        WeddingWebsite site = websiteRepository.findByCouple(couple.id)
                .orElseGet(() -> createNew(couple));
        site.coverPhotoUrl = photoUrl;
        return WeddingWebsiteDTO.Response.from(site);
    }

    // ── Authenticated preview (bypasses published gate) ────────────────────

    @Transactional
    public WeddingWebsiteDTO.PublicResponse getPreview(UUID coupleUserId) {
        CoupleProfile couple = requireCouple(coupleUserId);
        WeddingWebsite site = websiteRepository.findByCouple(couple.id)
                .orElseThrow(NotFoundException::new);
        return WeddingWebsiteDTO.PublicResponse.from(site);
    }

    // ── Public: get by subdomain ───────────────────────────────────────────

    @Transactional
    public WeddingWebsiteDTO.PublicResponse getPublic(String subdomain) {
        CoupleProfile couple = coupleRepository.findBySubdomain(subdomain)
                .orElseThrow(NotFoundException::new);
        WeddingWebsite site = websiteRepository.findByCouple(couple.id)
                .orElseThrow(NotFoundException::new);
        if (!site.published) throw new NotFoundException();
        return WeddingWebsiteDTO.PublicResponse.from(site);
    }

    // ── Increment RSVP counter ─────────────────────────────────────────────

    @Transactional
    public void incrementRsvpCount(CoupleProfile couple) {
        websiteRepository.findByCouple(couple.id)
                .ifPresent(site -> site.rsvpsSubmitted++);
    }

    // ── Track visitor ──────────────────────────────────────────────────────

    @Transactional
    public void trackVisit(String subdomain) {
        coupleRepository.findBySubdomain(subdomain)
                .flatMap(couple -> websiteRepository.findByCouple(couple.id))
                .ifPresent(site -> {
                    if (site.published) site.visitorCount++;
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isSubdomainAvailable(UUID coupleUserId, String slug) {
        if (slug == null || slug.isBlank()) return false;
        CoupleProfile couple = requireCouple(coupleUserId);
        String normalized = toSlug(slug);
        return coupleRepository.findBySubdomain(normalized)
                .map(other -> other.id.equals(couple.id))
                .orElse(true);
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private String generateUniqueSlug(CoupleProfile couple) {
        String base = Stream.of(couple.partner1Name, couple.partner2Name)
                .filter(n -> n != null && !n.isBlank())
                .map(n -> toSlug(firstWord(n)))
                .collect(Collectors.joining("-si-"));
        if (base.isBlank()) base = "nunta";
        // Keep trying until we land on a free slug (collision is astronomically rare)
        String slug;
        do {
            slug = base + "-" + randomSuffix(4);
        } while (coupleRepository.findBySubdomain(slug).isPresent());
        return slug;
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
        }
        return sb.toString();
    }

    private static String firstWord(String name) {
        return name.trim().split("\\s+")[0];
    }

    private CoupleProfile requireCouple(UUID userId) {
        return coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
    }

    private static String toSlug(String input) {
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

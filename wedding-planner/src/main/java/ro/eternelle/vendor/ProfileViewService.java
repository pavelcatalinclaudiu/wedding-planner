package ro.eternelle.vendor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Records a deduplicated view event for a vendor profile page.
 * Runs in its own transaction so it never rolls back the caller's response.
 */
@ApplicationScoped
public class ProfileViewService {

    @Inject
    ProfileViewRepository profileViewRepository;

    @Inject
    VendorRepository vendorRepository;

    @Inject
    ro.eternelle.user.UserRepository userRepository;

    @Inject
    VendorAnalyticsService vendorAnalyticsService;

    /**
     * Called from the vendor public endpoint after fetching the vendor.
     * The view is only persisted if the same viewer hasn't already been counted today.
     *
     * @param vendorId  the vendor profile id
     * @param viewerRaw the authenticated user's id, or null for anonymous
     * @param rawIp     the client IP address (may be null or X-Forwarded-For value)
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void recordView(UUID vendorId, UUID viewerRaw, String rawIp) {
        try {
            String ipHash = rawIp != null ? sha256Short(rawIp) : null;

            if (profileViewRepository.existsToday(vendorId, viewerRaw, ipHash)) {
                return;   // already counted today — skip
            }

            VendorProfile vendor = vendorRepository.findById(vendorId);
            if (vendor == null) return;

            ProfileView view = new ProfileView();
            view.vendor = vendor;
            view.viewer = viewerRaw != null ? userRepository.findById(viewerRaw) : null;
            view.ipHash = ipHash;
            profileViewRepository.persist(view);
            vendorAnalyticsService.invalidateOverview(vendorId);

        } catch (Exception ignored) {
            // Never surface view-tracking errors to the caller
        }
    }

    /** First 16 hex chars of SHA-256 — enough for dedup, not reversible. */
    private static String sha256Short(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

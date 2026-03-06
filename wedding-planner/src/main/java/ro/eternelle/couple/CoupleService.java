package ro.eternelle.couple;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;

import java.util.UUID;

@ApplicationScoped
public class CoupleService {

    @Inject
    CoupleRepository coupleRepository;

    @Inject
    UserRepository userRepository;

    public CoupleProfile getByUserId(UUID userId) {
        return coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
    }

    @Transactional
    public CoupleProfile createProfile(UUID userId, CoupleProfile profile) {
        User user = userRepository.findById(userId);
        if (user == null) throw new BusinessException("User not found");
        if (coupleRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException("Profile already exists");
        }
        profile.user = user;
        coupleRepository.persist(profile);
        return profile;
    }

    @Transactional
    public CoupleProfile updateProfile(UUID userId, CoupleProfile updates) {
        CoupleProfile profile = getByUserId(userId);
        if (updates.partner1Name != null)    profile.partner1Name    = updates.partner1Name;
        if (updates.partner2Name != null)    profile.partner2Name    = updates.partner2Name;
        if (updates.weddingDate != null)     profile.weddingDate     = updates.weddingDate;
        if (updates.weddingLocation != null) profile.weddingLocation = updates.weddingLocation;
        if (updates.totalBudget != null)     profile.totalBudget     = updates.totalBudget;
        profile.guestCount = updates.guestCount > 0 ? updates.guestCount : profile.guestCount;
        return profile;
    }
}

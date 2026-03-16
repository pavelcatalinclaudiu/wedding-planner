package ro.eternelle.vendor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.lead.LeadStatus;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;


import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class VendorService {

    @Inject
    VendorRepository vendorRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    LeadRepository leadRepository;

    @Inject
    AvailabilityRepository availabilityRepository;

    public VendorProfile getByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Vendor profile not found"));
    }

    public VendorProfile getById(UUID id) {
        VendorProfile vendor = vendorRepository.findById(id);
        if (vendor == null) throw new BusinessException("Vendor not found");
        if (!vendor.isActive) throw new BusinessException("Vendor not found");
        vendor.profileViews++;
        return vendor;
    }

    public List<VendorProfile> search(String category, String city, String keyword, String dateStr, String tier) {
        List<VendorProfile> results;
        if (category != null) {
            results = vendorRepository.findByCategory(VendorCategory.valueOf(category.toUpperCase()));
        } else if (city != null) {
            results = vendorRepository.findByCity(city);
        } else if (keyword != null) {
            results = vendorRepository.searchByKeyword(keyword);
        } else {
            results = vendorRepository.findActiveVendors();
        }

        // Filter by tier if requested (e.g. landing page fetching PREMIUM vendors)
        if (tier != null && !tier.isBlank()) {
            try {
                VendorTier t = VendorTier.valueOf(tier.toUpperCase());
                results = results.stream()
                        .filter(v -> v.monetizationEnabled && v.tier == t)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
                // Unknown tier value — skip filtering
            }
        }

        // Filter by date availability if requested
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(dateStr);
                Set<UUID> blockedVendorIds = availabilityRepository
                        .findBlockedVendorIds(date)
                        .stream().collect(Collectors.toSet());
                results = results.stream()
                        .filter(v -> !blockedVendorIds.contains(v.id))
                        .collect(Collectors.toList());
            } catch (Exception ignored) {
                // Malformed date — skip filtering
            }
        }

        // Premium vendors appear first, then Standard, then Free
        results.sort((a, b) -> {
            int tierA = a.monetizationEnabled ? a.tier.ordinal() : -1;
            int tierB = b.monetizationEnabled ? b.tier.ordinal() : -1;
            return Integer.compare(tierB, tierA);
        });

        return results;
    }

    @Transactional
    public VendorProfile createProfile(UUID userId, VendorProfile profile) {
        User user = userRepository.findById(userId);
        if (user == null) throw new BusinessException("User not found");
        if (vendorRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException("Profile already exists");
        }
        profile.user = user;
        vendorRepository.persist(profile);
        return profile;
    }

    @Transactional
    public VendorProfile updateProfile(UUID userId, VendorProfile updates) {
        VendorProfile profile = getByUserId(userId);
        if (updates.businessName != null)  profile.businessName  = updates.businessName;
        if (updates.category != null)       profile.category      = updates.category;
        if (updates.city != null)           profile.city          = updates.city;
        if (updates.description != null)    profile.description   = updates.description;
        if (updates.basePrice != null)      profile.basePrice     = updates.basePrice;
        profile.website   = updates.website;
        profile.instagram = updates.instagram;
        profile.facebook  = updates.facebook;
        return profile;
    }

    /**
     * Calculates the average time (in hours) a vendor takes to send their first
     * message after a couple files an enquiry.  Only deals that have received at
     * least one vendor reply are included in the average.
     */
    public double computeAvgResponseTimeHours(UUID vendorId) {
        List<Lead> leads = leadRepository.findByVendor(vendorId);
        if (leads.isEmpty()) return 0.0;

        List<Double> samples = leads.stream()
                .filter(l -> l.status != LeadStatus.NEW)
                .map(l -> (l.updatedAt.toEpochMilli() - l.createdAt.toEpochMilli()) / 3_600_000.0)
                .filter(h -> h >= 0)
                .collect(Collectors.toList());

        if (samples.isEmpty()) return 0.0;
        double avg = samples.stream().mapToDouble(d -> d).average().orElse(0.0);
        double rounded = Math.round(avg * 10.0) / 10.0;
        return rounded > 0 ? rounded : 0.1;
    }

    public VendorAnalyticsDTO getAnalytics(UUID userId) {
        VendorProfile vendor = getByUserId(userId);

        List<Lead> allLeads = leadRepository.findByVendor(vendor.id);
        int enquiries = allLeads.size();

        long confirmedCount = allLeads.stream()
                .filter(l -> l.status == LeadStatus.BOOKED)
                .count();

        double conversionRate = enquiries > 0
                ? Math.round((confirmedCount * 100.0 / enquiries) * 10.0) / 10.0
                : 0.0;

        double basePrice = vendor.basePrice != null ? vendor.basePrice.doubleValue() : 0.0;
        double revenue = basePrice * confirmedCount;

        double averageRating = vendor.avgRating != null ? vendor.avgRating.doubleValue() : 0.0;
        double avgResponseTimeHours = computeAvgResponseTimeHours(vendor.id);

        return new VendorAnalyticsDTO(
                vendor.profileViews,
                enquiries,
                conversionRate,
                revenue,
                avgResponseTimeHours,
                averageRating
        );
    }
}

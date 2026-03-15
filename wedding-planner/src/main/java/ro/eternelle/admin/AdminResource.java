package ro.eternelle.admin;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CouplePlan;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.review.Review;
import ro.eternelle.review.ReviewRepository;
import ro.eternelle.review.ReviewService;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.user.UserRole;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorTier;
import ro.eternelle.vendor.ProfileViewRepository;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.platform.PageVisitRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminResource {

    @Inject
    UserRepository userRepository;

    @Inject
    VendorRepository vendorRepository;

    @Inject
    CoupleRepository coupleRepository;

    @Inject
    LeadRepository leadRepository;

    @Inject
    ReviewRepository reviewRepository;

    @Inject
    ReviewService reviewService;

    @Inject
    ProfileViewRepository profileViewRepository;

    @Inject
    PageVisitRepository pageVisitRepository;

    @Inject
    EntityManager em;

    @Inject
    JsonWebToken jwt;

    // ─── Stats ────────────────────────────────────────────────────────────────

    @GET
    @Path("/stats")
    public Response getStats() {
        AdminStatsDTO dto = new AdminStatsDTO();

        dto.totalUsers = userRepository.count();
        dto.totalVendors = userRepository.count("role", UserRole.VENDOR);
        dto.totalCouples = userRepository.count("role", UserRole.COUPLE);

        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        dto.newUsersThisWeek = userRepository.count("createdAt >= ?1", weekAgo);

        dto.totalLeads = leadRepository.count();
        dto.activeVendors = vendorRepository.count("isActive = true");
        dto.totalReviews = reviewRepository.count();

        // Profile view stats
        dto.totalProfileViews = profileViewRepository.count();
        dto.profileViewsThisWeek = profileViewRepository.count("viewedAt >= ?1", weekAgo);
        Number uniqueVisitorCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM (" +
                "  SELECT viewer_id AS uid FROM profile_views WHERE viewer_id IS NOT NULL" +
                "  UNION" +
                "  SELECT ip_hash   AS uid FROM profile_views WHERE viewer_id IS NULL AND ip_hash IS NOT NULL" +
                ") AS visitors")
                .getSingleResult();
        dto.uniqueVisitors = uniqueVisitorCount != null ? uniqueVisitorCount.longValue() : 0;

        // Landing page visit stats
        dto.landingPageVisits = pageVisitRepository.count();
        dto.landingPageVisitsThisWeek = pageVisitRepository.count("visitedAt >= ?1", weekAgo);
        Number uniqueLandingCount = (Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT ip_hash) FROM page_visits WHERE ip_hash IS NOT NULL")
                .getSingleResult();
        dto.uniqueLandingVisitors = uniqueLandingCount != null ? uniqueLandingCount.longValue() : 0;

        // Average rating across all vendors
        Double avgRating = (Double) em.createQuery(
                "SELECT AVG(v.avgRating) FROM VendorProfile v WHERE v.reviewCount > 0")
                .getSingleResult();
        dto.avgRating = avgRating != null ? avgRating : 0.0;

        // Signups by day — last 30 days using JPQL date truncations
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        @SuppressWarnings("unchecked")
        List<Object[]> rawDays = em.createNativeQuery(
                "SELECT DATE(created_at) AS day, COUNT(*) AS cnt " +
                "FROM users WHERE created_at >= :since " +
                "GROUP BY day ORDER BY day")
                .setParameter("since", thirtyDaysAgo)
                .getResultList();

        dto.signupsByDay = rawDays.stream()
                .map(row -> new AdminStatsDTO.DailySignup(row[0].toString(),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // Vendors by category
        @SuppressWarnings("unchecked")
        List<Object[]> catRows = em.createNativeQuery(
                "SELECT category, COUNT(*) FROM vendor_profiles GROUP BY category")
                .getResultList();
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] r : catRows) {
            if (r[0] != null) byCategory.put(r[0].toString(), ((Number) r[1]).longValue());
        }
        dto.vendorsByCategory = byCategory;

        // Top 5 vendors by review count
        @SuppressWarnings("unchecked")
        List<Object[]> topRows = em.createNativeQuery(
                "SELECT vp.id, vp.business_name, vp.category, vp.city, vp.avg_rating, vp.review_count, " +
                "  (SELECT COUNT(*) FROM leads l WHERE l.vendor_id = vp.id) AS lead_cnt " +
                "FROM vendor_profiles vp ORDER BY vp.review_count DESC, vp.avg_rating DESC LIMIT 5")
                .getResultList();

        dto.topVendors = topRows.stream().map(r -> new AdminStatsDTO.TopVendor(
                r[0].toString(),
                r[1] != null ? r[1].toString() : "",
                r[2] != null ? r[2].toString() : "",
                r[3] != null ? r[3].toString() : "",
                r[4] != null ? ((Number) r[4]).doubleValue() : 0.0,
                r[5] != null ? ((Number) r[5]).intValue() : 0,
                r[6] != null ? ((Number) r[6]).intValue() : 0
        )).collect(Collectors.toList());

        dto.pendingReviewsCount = reviewRepository.count("status = 'PENDING'");
        dto.paidVendors = (long) em.createQuery(
                "SELECT COUNT(v) FROM VendorProfile v WHERE v.tier != 'FREE'").getSingleResult();
        dto.paidCouples = (long) em.createQuery(
                "SELECT COUNT(c) FROM CoupleProfile c WHERE c.plan != 'FREE'").getSingleResult();

        return Response.ok(dto).build();
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    @GET
    @Path("/users")
    public Response listUsers(
            @QueryParam("role") String role,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        String jpql = "SELECT u FROM User u WHERE 1=1";
        Map<String, Object> params = new LinkedHashMap<>();

        if (role != null && !role.isBlank()) {
            try {
                UserRole r = UserRole.valueOf(role.toUpperCase());
                jpql += " AND u.role = :role";
                params.put("role", r);
            } catch (IllegalArgumentException ignored) {}
        }
        if (search != null && !search.isBlank()) {
            jpql += " AND lower(u.email) like :search";
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        jpql += " ORDER BY u.createdAt DESC";

        var query = em.createQuery(jpql, User.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<User> users = query.getResultList();

        List<AdminUserDTO> result = users.stream().map(u -> {
            CoupleProfile cp = coupleRepository.findByUserId(u.id).orElse(null);
            VendorProfile vp = vendorRepository.findByUserId(u.id).orElse(null);
            return AdminUserDTO.from(u, cp, vp);
        }).collect(Collectors.toList());

        var countQuery = em.createQuery(
                jpql.replace("SELECT u ", "SELECT COUNT(u) ").replace(" ORDER BY u.createdAt DESC", ""), Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", result);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return Response.ok(resp).build();
    }

    @DELETE
    @Path("/users/{id}")
    @Transactional
    public Response deleteUser(@PathParam("id") UUID userId) {
        // Prevent admin from deleting themselves
        UUID selfId = UUID.fromString(jwt.getSubject());
        if (selfId.equals(userId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Cannot delete your own admin account"))
                    .build();
        }
        User user = userRepository.findById(userId);
        if (user == null) return Response.status(Response.Status.NOT_FOUND).build();
        userRepository.delete(user);
        return Response.noContent().build();
    }

    // ─── Vendors ──────────────────────────────────────────────────────────────

    @GET
    @Path("/vendors")
    public Response listVendors(
            @QueryParam("search") String search,
            @QueryParam("category") String category,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        String jpql = "SELECT v FROM VendorProfile v WHERE 1=1";
        Map<String, Object> params = new LinkedHashMap<>();

        if (search != null && !search.isBlank()) {
            jpql += " AND (lower(v.businessName) like :search OR lower(v.city) like :search)";
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        if (category != null && !category.isBlank()) {
            try {
                ro.eternelle.vendor.VendorCategory cat = ro.eternelle.vendor.VendorCategory.valueOf(category.toUpperCase());
                jpql += " AND v.category = :category";
                params.put("category", cat);
            } catch (IllegalArgumentException ignored) {}
        }
        jpql += " ORDER BY v.createdAt DESC";

        var query = em.createQuery(jpql, VendorProfile.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<VendorProfile> vendors = query.getResultList();

        List<AdminVendorDTO> result = vendors.stream()
                .map(v -> AdminVendorDTO.from(v, leadRepository.countByVendor(v.id)))
                .collect(Collectors.toList());

        var vendorCountQuery = em.createQuery(
                jpql.replace("SELECT v ", "SELECT COUNT(v) ").replace(" ORDER BY v.createdAt DESC", ""), Long.class);
        params.forEach(vendorCountQuery::setParameter);
        long total = vendorCountQuery.getSingleResult();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", result);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return Response.ok(resp).build();
    }

    @PUT
    @Path("/vendors/{id}/suspend")
    @Transactional
    public Response suspendVendor(@PathParam("id") UUID vendorId) {
        VendorProfile v = vendorRepository.findById(vendorId);
        if (v == null) return Response.status(Response.Status.NOT_FOUND).build();
        v.isActive = false;
        return Response.ok(Map.of("isActive", false)).build();
    }

    @PUT
    @Path("/vendors/{id}/activate")
    @Transactional
    public Response activateVendor(@PathParam("id") UUID vendorId) {
        VendorProfile v = vendorRepository.findById(vendorId);
        if (v == null) return Response.status(Response.Status.NOT_FOUND).build();
        v.isActive = true;
        return Response.ok(Map.of("isActive", true)).build();
    }

    @PUT
    @Path("/vendors/{id}/verify")
    @Transactional
    public Response verifyVendor(@PathParam("id") UUID vendorId) {
        VendorProfile v = vendorRepository.findById(vendorId);
        if (v == null) return Response.status(Response.Status.NOT_FOUND).build();
        v.isVerified = !v.isVerified;
        return Response.ok(Map.of("isVerified", v.isVerified)).build();
    }

    @PUT
    @Path("/vendors/{id}/plan")
    @Transactional
    public Response setVendorPlan(@PathParam("id") UUID vendorId,
                                   @QueryParam("plan") String plan) {
        VendorProfile v = vendorRepository.findById(vendorId);
        if (v == null) return Response.status(Response.Status.NOT_FOUND).build();
        try {
            v.tier = VendorTier.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Invalid plan: " + plan)).build();
        }
        return Response.ok(Map.of("tier", v.tier.name())).build();
    }

    @PUT
    @Path("/couples/{id}/plan")
    @Transactional
    public Response setCouplePlan(@PathParam("id") UUID coupleId,
                                   @QueryParam("plan") String plan) {
        CoupleProfile c = coupleRepository.findById(coupleId);
        if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
        try {
            c.plan = CouplePlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Invalid plan: " + plan)).build();
        }
        return Response.ok(Map.of("plan", c.plan.name())).build();
    }

    @PUT
    @Path("/vendors/{id}/monetization")
    @Transactional
    public Response toggleVendorMonetization(@PathParam("id") UUID vendorId,
                                              @QueryParam("enabled") boolean enabled) {
        VendorProfile v = vendorRepository.findById(vendorId);
        if (v == null) return Response.status(Response.Status.NOT_FOUND).build();
        v.monetizationEnabled = enabled;
        return Response.ok(Map.of("monetizationEnabled", v.monetizationEnabled)).build();
    }

    @PUT
    @Path("/couples/{id}/monetization")
    @Transactional
    public Response toggleCoupleMonetization(@PathParam("id") UUID coupleId,
                                              @QueryParam("enabled") boolean enabled) {
        CoupleProfile c = coupleRepository.findById(coupleId);
        if (c == null) return Response.status(Response.Status.NOT_FOUND).build();
        c.monetizationEnabled = enabled;
        return Response.ok(Map.of("monetizationEnabled", c.monetizationEnabled)).build();
    }

    @PUT
    @Path("/vendors/monetization/bulk")
    @Transactional
    public Response bulkToggleVendorMonetization(@QueryParam("enabled") boolean enabled) {
        em.createQuery("UPDATE VendorProfile v SET v.monetizationEnabled = :enabled")
                .setParameter("enabled", enabled)
                .executeUpdate();
        return Response.ok(Map.of("enabled", enabled)).build();
    }

    @PUT
    @Path("/couples/monetization/bulk")
    @Transactional
    public Response bulkToggleCoupleMonetization(@QueryParam("enabled") boolean enabled) {
        em.createQuery("UPDATE CoupleProfile c SET c.monetizationEnabled = :enabled")
                .setParameter("enabled", enabled)
                .executeUpdate();
        return Response.ok(Map.of("enabled", enabled)).build();
    }

    // ─── Reviews ──────────────────────────────────────────────────────────────

    @GET
    @Path("/reviews")
    public Response listReviews(
            @QueryParam("search") String search,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        String jpql = "SELECT r FROM Review r WHERE 1=1";
        Map<String, Object> params = new LinkedHashMap<>();

        if (search != null && !search.isBlank()) {
            jpql += " AND (lower(r.vendor.businessName) like :search OR lower(r.comment) like :search)";
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        if (status != null && !status.isBlank()) {
            jpql += " AND r.status = :status";
            params.put("status", status.toUpperCase());
        }
        jpql += " ORDER BY r.createdAt DESC";

        var query = em.createQuery(jpql, Review.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<Review> reviews = query.getResultList();

        List<AdminReviewDTO> result = reviews.stream()
                .map(AdminReviewDTO::from)
                .collect(Collectors.toList());

        var reviewCountQuery = em.createQuery(
                jpql.replace("SELECT r ", "SELECT COUNT(r) ").replace(" ORDER BY r.createdAt DESC", ""), Long.class);
        params.forEach(reviewCountQuery::setParameter);
        long total = reviewCountQuery.getSingleResult();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", result);
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        return Response.ok(resp).build();
    }

    @DELETE
    @Path("/reviews/{id}")
    @Transactional
    public Response deleteReview(@PathParam("id") UUID reviewId) {
        Review review = reviewRepository.findById(reviewId);
        if (review == null) return Response.status(Response.Status.NOT_FOUND).build();
        reviewRepository.delete(review);
        return Response.noContent().build();
    }

    @PUT
    @Path("/reviews/{id}/approve")
    public Response approveReview(@PathParam("id") UUID reviewId) {
        return Response.ok(reviewService.approveReview(reviewId)).build();
    }

    @PUT
    @Path("/reviews/{id}/reject")
    public Response rejectReview(@PathParam("id") UUID reviewId) {
        return Response.ok(reviewService.rejectReview(reviewId)).build();
    }
}

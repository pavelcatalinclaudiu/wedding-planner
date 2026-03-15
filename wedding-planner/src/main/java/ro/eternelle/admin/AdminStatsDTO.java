package ro.eternelle.admin;

import java.util.List;
import java.util.Map;

public class AdminStatsDTO {

    public long totalUsers;
    public long totalVendors;
    public long totalCouples;
    public long newUsersThisWeek;
    public long totalLeads;
    public long activeVendors;
    public long totalReviews;
    public double avgRating;

    /** Total vendor profile page views across the whole platform */
    public long totalProfileViews;
    /** Distinct visitors (by ip_hash or viewer_id) */
    public long uniqueVisitors;
    /** Profile views in the last 7 days */
    public long profileViewsThisWeek;

    /** Total landing-page visits (home page) */
    public long landingPageVisits;
    /** Distinct IPs that hit the landing page (all-time) */
    public long uniqueLandingVisitors;
    /** Landing-page visits in the last 7 days */
    public long landingPageVisitsThisWeek;

    /** [{date: "2024-01-15", count: 5}, ...] — last 30 days */
    public List<DailySignup> signupsByDay;

    /** {"PHOTOGRAPHER": 12, "FLORIST": 4, ...} */
    public Map<String, Long> vendorsByCategory;

    /** Top 5 vendors by review count */
    public List<TopVendor> topVendors;

    /** Reviews awaiting moderation */
    public long pendingReviewsCount;

    /** Vendors on STANDARD or PREMIUM plan */
    public long paidVendors;

    /** Couples on DREAM_WEDDING plan */
    public long paidCouples;

    public static class DailySignup {
        public String date;
        public long count;

        public DailySignup(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }

    public static class TopVendor {
        public String id;
        public String businessName;
        public String category;
        public String city;
        public double avgRating;
        public int reviewCount;
        public int leadCount;

        public TopVendor(String id, String businessName, String category,
                         String city, double avgRating, int reviewCount, int leadCount) {
            this.id = id;
            this.businessName = businessName;
            this.category = category;
            this.city = city;
            this.avgRating = avgRating;
            this.reviewCount = reviewCount;
            this.leadCount = leadCount;
        }
    }
}

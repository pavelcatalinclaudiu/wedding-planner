package ro.eternelle.vendor;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.booking.BookingRepository;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.lead.LeadStatus;
import ro.eternelle.review.Review;
import ro.eternelle.review.ReviewRepository;
import ro.eternelle.vendor.dto.*;
import ro.eternelle.videocall.VideoCall;
import ro.eternelle.videocall.VideoCallRepository;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@ApplicationScoped
public class VendorAnalyticsService {

    @Inject BookingRepository     bookingRepository;
    @Inject LeadRepository        leadRepository;
    @Inject ProfileViewRepository profileViewRepository;
    @Inject ReviewRepository      reviewRepository;
    @Inject VideoCallRepository   videoCallRepository;
    @Inject VendorRepository      vendorRepository;
    @Inject VendorService         vendorService;

    //  Entry point 

    @CacheResult(cacheName = "vendor-overview")
    @Transactional
    public VendorOverviewDTO buildOverview(@CacheKey UUID vendorId, UUID vendorUserId) {
        return new VendorOverviewDTO(
                getRevenueBooked(vendorId),
                getLeadsStat(vendorId),
                getViewsStat(vendorId),
                getRatingStat(vendorId),
                getResponseRate(vendorId, vendorUserId),
                getMonthlyLeads(vendorId),
                getRecentLeads(vendorId, 3),
                getUpcomingCalls(vendorId, 2),
                getProfilePerformance(vendorId)
        );
    }

    @CacheInvalidate(cacheName = "vendor-overview")
    public void invalidateOverview(@CacheKey UUID vendorId) {}

    //  Stat 1  Revenue Booked 

    public RevenueStatDTO getRevenueBooked(UUID vendorId) {
        Instant monthStart    = YearMonth.now().atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal current  = bookingRepository.sumRevenueForVendorSince(vendorId, monthStart);
        BigDecimal previous = bookingRepository.sumRevenueForVendorBetween(vendorId, prevMonthStart, monthStart);
        long       count    = bookingRepository.countConfirmedForVendorSince(vendorId, monthStart);
        BigDecimal trend    = current.subtract(previous);

        return new RevenueStatDTO(current, trend, count);
    }

    //  Stat 2  Total Leads 

    public LeadsStatDTO getLeadsStat(UUID vendorId) {
        Instant monthStart     = YearMonth.now().atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayStart       = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);

        long total     = leadRepository.countByVendor(vendorId);
        long thisMonth = leadRepository.countByVendorSince(vendorId, monthStart);
        long prevMonth = leadRepository.countByVendorBetween(vendorId, prevMonthStart, monthStart);
        long today     = leadRepository.countByVendorSince(vendorId, dayStart);
        long trend     = thisMonth - prevMonth;

        return new LeadsStatDTO(total, thisMonth, today, trend);
    }

    //  Stat 3  Profile Views 

    public ViewsStatDTO getViewsStat(UUID vendorId) {
        Instant weekStart     = LocalDate.now(ZoneOffset.UTC).with(DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevWeekStart = weekStart.minus(7, ChronoUnit.DAYS);

        long total    = profileViewRepository.countByVendor(vendorId);
        long thisWeek = profileViewRepository.countByVendorSince(vendorId, weekStart);
        long prevWeek = profileViewRepository.countByVendorBetween(vendorId, prevWeekStart, weekStart);

        long trendPct = prevWeek == 0
                ? (thisWeek > 0 ? 100L : 0L)
                : Math.round(((double)(thisWeek - prevWeek) / prevWeek) * 100);

        return new ViewsStatDTO(total, thisWeek, trendPct);
    }

    //  Stat 4  Average Rating 

    public RatingStatDTO getRatingStat(UUID vendorId) {
        List<Review> reviews = reviewRepository.findByVendor(vendorId);
        if (reviews.isEmpty()) return new RatingStatDTO(0.0, 0, null);

        double avg = reviews.stream().mapToDouble(r -> r.rating.doubleValue()).average().orElse(0.0);
        double rounded = Math.round(avg * 10.0) / 10.0;

        Review latest = reviews.get(0);
        long daysAgo  = ChronoUnit.DAYS.between(latest.createdAt, Instant.now());

        return new RatingStatDTO(rounded, reviews.size(), daysAgo);
    }

    //  Stat 5  Response Rate 

    public ResponseRateDTO getResponseRate(UUID vendorId, UUID vendorUserId) {
        Instant now           = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant sixtyDaysAgo  = now.minus(60, ChronoUnit.DAYS);

        long[] current  = parseResponseRateRow(leadRepository.calculateResponseRate(vendorId, thirtyDaysAgo, now));
        long[] previous = parseResponseRateRow(leadRepository.calculateResponseRate(vendorId, sixtyDaysAgo, thirtyDaysAgo));

        int currentRate  = current[0]  == 0 ? 100 : (int) Math.round((double) current[1]  / current[0]  * 100);
        int previousRate = previous[0] == 0 ? 100 : (int) Math.round((double) previous[1] / previous[0] * 100);

        return new ResponseRateDTO(currentRate, currentRate - previousRate, current[1], current[0]);
    }

    private long[] parseResponseRateRow(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return new long[]{0, 0};
        Object[] row = rows.get(0);
        long total  = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long timely = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        return new long[]{total, timely};
    }

    //  Stat 6  Monthly Leads Chart (last 7 months) 

    public List<MonthlyLeadDTO> getMonthlyLeads(UUID vendorId) {
        Instant since = YearMonth.now().minusMonths(6).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> rows = leadRepository.countByVendorGroupedByMonth(vendorId, since);

        Map<YearMonth, Long> countByMonth = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) countByMonth.put(YearMonth.now().minusMonths(i), 0L);

        for (Object[] row : rows) {
            try {
                YearMonth ym = YearMonth.parse(row[0].toString().substring(0, 7));
                if (countByMonth.containsKey(ym)) countByMonth.put(ym, ((Number) row[1]).longValue());
            } catch (Exception ignored) {}
        }

        YearMonth current = YearMonth.now();
        return countByMonth.entrySet().stream()
                .map(e -> new MonthlyLeadDTO(
                        e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        e.getValue(),
                        e.getKey().equals(current)))
                .toList();
    }

    //  Widget  Recent Leads 

    public List<RecentLeadDTO> getRecentLeads(UUID vendorId, int limit) {
        List<Lead> leads = leadRepository.findRecentByVendor(vendorId, limit);
        List<RecentLeadDTO> result = new ArrayList<>();

        for (Lead lead : leads) {
            String coupleName  = buildCoupleName(lead);
            String weddingDate = lead.couple != null && lead.couple.weddingDate != null ? lead.couple.weddingDate.toString() : null;
            String location    = lead.couple != null ? lead.couple.weddingLocation : null;
            String preview     = lead.message != null ? truncate(lead.message, 120) : "";

            result.add(new RecentLeadDTO(
                    lead.id.toString(),
                    coupleName,
                    weddingDate,
                    location,
                    preview,
                    lead.createdAt,
                    lead.status.name()
            ));
        }
        return result;
    }

    //  Widget  Upcoming Calls 

    public List<UpcomingCallDTO> getUpcomingCalls(UUID vendorId, int limit) {
        List<VideoCall> calls = videoCallRepository.findUpcomingByVendor(vendorId, limit);
        List<UpcomingCallDTO> result = new ArrayList<>();

        for (VideoCall call : calls) {
            String coupleName = call.lead != null ? buildCoupleName(call.lead) : "Unknown";
            result.add(new UpcomingCallDTO(
                    call.id.toString(),
                    coupleName,
                    call.scheduledAt,
                    call.lead != null ? call.lead.id.toString() : null
            ));
        }
        return result;
    }

    //  Widget  Profile Performance 

    public ProfilePerformanceDTO getProfilePerformance(UUID vendorId) {
        VendorProfile vendor = vendorRepository.findById(vendorId);
        if (vendor == null) return new ProfilePerformanceDTO(0L, "", 0.0, 0, 12);

        long higher = vendorRepository.countWithHigherRating(vendor.category, vendor.city, vendor.avgRating);
        long rank   = higher + 1;
        String catLabel  = vendor.category != null ? vendor.category.name().replace("_", " ") : "Vendor";
        String cityLabel = vendor.city != null ? vendor.city : "";
        String rankCategory = catLabel + (cityLabel.isBlank() ? "" : "  " + cityLabel);

        double avgResponseTime = vendorService.computeAvgResponseTimeHours(vendorId);

        long total     = leadRepository.countByVendor(vendorId);
        long booked    = leadRepository.findByVendorAndStatus(vendorId, LeadStatus.BOOKED).size();
        int  conversionRate = total == 0 ? 0 : (int) Math.round((double) booked / total * 100);

        return new ProfilePerformanceDTO(rank, rankCategory, avgResponseTime, conversionRate, 12);
    }

    //  Deep Analytics (Analytics page) ───────────────────────────────────────

    @Transactional
    public VendorDeepAnalyticsDTO getDeepAnalytics(UUID vendorId) {
        return new VendorDeepAnalyticsDTO(
                buildRevenueByMonth(vendorId),
                buildLeadFunnel(vendorId),
                buildRatingByQuarter(vendorId),
                buildBookingPeaks(vendorId)
        );
    }

    private List<VendorDeepAnalyticsDTO.MonthlyRevenueDTO> buildRevenueByMonth(UUID vendorId) {
        Instant since = YearMonth.now().minusMonths(11).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> rows = bookingRepository.revenueGroupedByMonth(vendorId, since);

        // Build a complete 12-month scaffold (no gaps)
        Map<YearMonth, double[]> map = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) map.put(YearMonth.now().minusMonths(i), new double[]{0, 0});

        for (Object[] row : rows) {
            try {
                YearMonth ym = YearMonth.parse(row[0].toString());
                if (map.containsKey(ym)) {
                    map.get(ym)[0] = ((Number) row[1]).doubleValue();
                    map.get(ym)[1] = ((Number) row[2]).doubleValue();
                }
            } catch (Exception ignored) {}
        }

        return map.entrySet().stream().map(e -> new VendorDeepAnalyticsDTO.MonthlyRevenueDTO(
                e.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                e.getKey().getYear(),
                Math.round(e.getValue()[0] * 100.0) / 100.0,
                (long) e.getValue()[1]
        )).toList();
    }

    private VendorDeepAnalyticsDTO.LeadFunnelDTO buildLeadFunnel(UUID vendorId) {
        List<Lead> leads = leadRepository.findByVendor(vendorId);
        long newLeads       = leads.stream().filter(l -> l.status == LeadStatus.NEW || l.status == LeadStatus.VIEWED).count();
        long inDiscussion   = leads.stream().filter(l -> l.status == LeadStatus.IN_DISCUSSION).count();
        long quoted         = leads.stream().filter(l -> l.status == LeadStatus.QUOTED).count();
        long booked         = leads.stream().filter(l -> l.status == LeadStatus.BOOKED).count();
        long declined       = leads.stream().filter(l -> l.status == LeadStatus.DECLINED).count();
        return new VendorDeepAnalyticsDTO.LeadFunnelDTO(newLeads, inDiscussion, quoted, booked, declined);
    }

    private List<VendorDeepAnalyticsDTO.QuarterlyRatingDTO> buildRatingByQuarter(UUID vendorId) {
        List<Review> reviews = reviewRepository.findByVendor(vendorId);

        // Group by "Q1 YYYY", "Q2 YYYY" etc. for the last 4 quarters
        Map<String, List<Double>> buckets = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 3; i >= 0; i--) {
            YearMonth qStart = now.minusMonths((long) i * 3);
            int q = (qStart.getMonthValue() - 1) / 3 + 1;
            buckets.put("Q" + q + " " + qStart.getYear(), new ArrayList<>());
        }

        for (Review r : reviews) {
            LocalDate d = r.createdAt.atZone(ZoneOffset.UTC).toLocalDate();
            YearMonth ym = YearMonth.of(d.getYear(), d.getMonth());
            int q = (ym.getMonthValue() - 1) / 3 + 1;
            String key = "Q" + q + " " + ym.getYear();
            if (buckets.containsKey(key)) {
                buckets.get(key).add(r.rating.doubleValue());
            }
        }

        return buckets.entrySet().stream().map(e -> {
            List<Double> ratings = e.getValue();
            double avg = ratings.isEmpty() ? 0.0
                    : Math.round(ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 10.0) / 10.0;
            return new VendorDeepAnalyticsDTO.QuarterlyRatingDTO(e.getKey(), avg, ratings.size());
        }).toList();
    }

    private List<VendorDeepAnalyticsDTO.BookingPeakDTO> buildBookingPeaks(UUID vendorId) {
        List<Lead> booked = leadRepository.findByVendorAndStatus(vendorId, LeadStatus.BOOKED);
        long[] counts = new long[13]; // index 1–12

        for (Lead l : booked) {
            if (l.eventDate != null) {
                counts[l.eventDate.getMonthValue()]++;
            }
        }

        List<VendorDeepAnalyticsDTO.BookingPeakDTO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String name = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new VendorDeepAnalyticsDTO.BookingPeakDTO(name, m, counts[m]));
        }
        return result;
    }

    //  Helpers 

    private String buildCoupleName(Lead lead) {
        if (lead.couple == null) return "Unknown";
        String p1 = lead.couple.partner1Name;
        String p2 = lead.couple.partner2Name;
        if (p1 == null && p2 == null) return "Unknown";
        if (p2 == null) return p1;
        if (p1 == null) return p2;
        return p1 + " & " + p2;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "";
    }
}

package ro.eternelle.vendor;

public class VendorAnalyticsDTO {

    public int profileViews;
    public int enquiries;
    public double conversionRate;
    public double revenue;
    public double avgResponseTimeHours;
    public double averageRating;

    public VendorAnalyticsDTO() {}

    public VendorAnalyticsDTO(
            int profileViews,
            int enquiries,
            double conversionRate,
            double revenue,
            double avgResponseTimeHours,
            double averageRating) {
        this.profileViews = profileViews;
        this.enquiries = enquiries;
        this.conversionRate = conversionRate;
        this.revenue = revenue;
        this.avgResponseTimeHours = avgResponseTimeHours;
        this.averageRating = averageRating;
    }
}

package ro.eternelle.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorRepository;

import java.util.List;

@ApplicationScoped
public class AnalyticsRefreshScheduler {

    @Inject
    VendorRepository vendorRepository;

    /**
     * Resets daily profile view counts every midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyViews() {
        // In a real app this would persist daily snapshots before resetting.
        // For now we just let the counter stay cumulative.
        // Placeholder for future analytics aggregation logic.
    }
}

package ro.eternelle.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.videocall.VideoCall;
import ro.eternelle.videocall.VideoCallRepository;
import ro.eternelle.videocall.VideoCallStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class CallReminderScheduler {

    private static final Logger LOG = Logger.getLogger(CallReminderScheduler.class);

    @Inject VideoCallRepository videoCallRepository;
    @Inject NotificationService notificationService;

    /**
     * Every 15 minutes — send a reminder for calls starting within the next hour.
     * Must be @Transactional so lazy associations (lead.couple.user, lead.vendor)
     * are loaded within the same persistence context / connection.
     */
    @Scheduled(every = "15m")
    @Transactional
    public void sendReminders() {
        Instant now = Instant.now();
        Instant horizon = now.plus(60, ChronoUnit.MINUTES);
        List<VideoCall> upcoming = videoCallRepository.findScheduledBetween(now, horizon);

        for (VideoCall call : upcoming) {
            notificationService.create(
                    call.lead.couple.user.id,
                    "call_reminder",
                    "Video call in less than 1 hour",
                    "Your call with " + call.lead.vendor.businessName + " is starting soon.",
                    call.id
            );
            notificationService.create(
                    call.lead.vendor.user.id,
                    "call_reminder",
                    "Video call in less than 1 hour",
                    "Your call with " + call.lead.couple.partner1Name + " is starting soon.",
                    call.id
            );
        }
    }

    /**
     * Every 5 minutes — mark SCHEDULED or IN_PROGRESS calls as COMPLETED once
     * their 30-minute window has expired (scheduledAt + 30 min < now).
     */
    @Scheduled(every = "5m")
    @Transactional
    public void autoCompleteExpiredCalls() {
        if (Thread.currentThread().isInterrupted()) {
            LOG.debug("autoCompleteExpiredCalls skipped — worker thread interrupted");
            return;
        }
        try {
            Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
            List<VideoCall> expired = videoCallRepository.findExpiredActive(cutoff);
            for (VideoCall call : expired) {
                call.status = VideoCallStatus.COMPLETED;
                call.endedAt = call.scheduledAt.plus(30, ChronoUnit.MINUTES);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (isInterruption(cause)) {
                LOG.warn("autoCompleteExpiredCalls interrupted while acquiring JDBC connection — will retry next cycle");
                Thread.currentThread().interrupt();
            } else {
                throw e;
            }
        }
    }

    private static boolean isInterruption(Throwable t) {
        while (t != null) {
            if (t instanceof InterruptedException) return true;
            String msg = t.getMessage();
            if (msg != null && msg.toLowerCase().contains("interrupt")) return true;
            t = t.getCause();
        }
        return false;
    }
}

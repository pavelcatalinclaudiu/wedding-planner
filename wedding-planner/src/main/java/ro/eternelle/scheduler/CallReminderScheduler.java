package ro.eternelle.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.videocall.VideoCall;
import ro.eternelle.videocall.VideoCallRepository;
import ro.eternelle.videocall.VideoCallStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class CallReminderScheduler {

    @Inject VideoCallRepository videoCallRepository;
    @Inject NotificationService notificationService;

    /**
     * Every 15 minutes — send a reminder for calls starting within the next hour.
     */
    @Scheduled(every = "15m")
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
        Instant cutoff = Instant.now().minus(30, ChronoUnit.MINUTES);
        List<VideoCall> expired = videoCallRepository.findExpiredActive(cutoff);
        for (VideoCall call : expired) {
            call.status = VideoCallStatus.COMPLETED;
            call.endedAt = call.scheduledAt.plus(30, ChronoUnit.MINUTES);
        }
    }
}

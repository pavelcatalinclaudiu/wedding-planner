package ro.eternelle.videocall;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.notification.NotificationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VideoCallService {

    private static final String JITSI_PUBLIC_BASE = "https://meet.jit.si/";
    private static final String JAAS_BASE         = "https://8x8.vc/";

    @Inject VideoCallRepository videoCallRepository;
    @Inject LeadRepository      leadRepository;
    @Inject NotificationService notificationService;

    @ConfigProperty(name = "jaas.app.id", defaultValue = "your-jaas-app-id")
    String jaasAppId;

    private boolean jaasEnabled() {
        return jaasAppId != null && !jaasAppId.isBlank() && !jaasAppId.equals("your-jaas-app-id");
    }

    public List<VideoCall> getByLead(UUID leadId) {
        return videoCallRepository.findByLead(leadId);
    }

    /** Returns the most recent PENDING, SCHEDULED, or IN_PROGRESS call for the lead, or empty. */
    public Optional<VideoCall> getActiveForLead(UUID leadId) {
        return videoCallRepository.findByLead(leadId).stream()
                .filter(c -> c.status == VideoCallStatus.PENDING
                          || c.status == VideoCallStatus.SCHEDULED
                          || c.status == VideoCallStatus.IN_PROGRESS)
                .max(java.util.Comparator.comparing(c -> c.scheduledAt));
    }

    public List<VideoCall> getForUser(UUID userId, boolean isVendor) {
        return isVendor
                ? videoCallRepository.findByVendorUserId(userId)
                : videoCallRepository.findByCoupleUserId(userId);
    }

    @Transactional
    public VideoCall schedule(UUID leadId, Instant scheduledAt, PostCallAction postCallAction, boolean proposedByVendor) {
        Lead lead = leadRepository.findById(leadId);
        if (lead == null) throw new BusinessException("Lead not found");

        String roomName = "eternelle-" + UUID.randomUUID().toString().replace("-", "");
        String roomUrl  = jaasEnabled()
                ? JAAS_BASE + jaasAppId + "/" + roomName
                : JITSI_PUBLIC_BASE + roomName;

        VideoCall call       = new VideoCall();
        call.lead            = lead;
        call.roomName        = roomName;
        call.roomUrl         = roomUrl;
        call.scheduledAt     = scheduledAt;
        call.postCallAction  = postCallAction;
        call.status          = VideoCallStatus.PENDING;
        call.proposedBy      = proposedByVendor ? "VENDOR" : "COUPLE";
        videoCallRepository.persist(call);

        // Notify both parties
        String vendorBiz  = lead.vendor != null ? lead.vendor.businessName : "your vendor";
        String coupleName = lead.couple != null
                ? (lead.couple.partner1Name + (lead.couple.partner2Name != null ? " & " + lead.couple.partner2Name : ""))
                : "the couple";
        String proposer   = proposedByVendor ? vendorBiz : coupleName;
        String recipient  = proposedByVendor ? coupleName : vendorBiz;

        if (lead.couple != null && lead.couple.user != null) {
            String msg = proposedByVendor
                    ? vendorBiz + " proposed a video call. Please confirm a time."
                    : "Your video call request was sent to " + vendorBiz + " for confirmation.";
            notificationService.create(lead.couple.user.id, "VIDEO_CALL_PROPOSED",
                    "Video Call Request", msg, call.id);
        }
        if (lead.vendor != null && lead.vendor.user != null) {
            String msg = proposedByVendor
                    ? "Your video call request was sent to " + coupleName + " for confirmation."
                    : coupleName + " proposed a video call. Please confirm a time.";
            notificationService.create(lead.vendor.user.id, "VIDEO_CALL_PROPOSED",
                    "Video Call Request", msg, call.id);
        }

        return call;
    }

    @Transactional
    public VideoCall accept(UUID callId, UUID requestingUserId) {
        VideoCall call = findById(callId);
        if (call.status != VideoCallStatus.PENDING) {
            throw new BusinessException("Only pending calls can be accepted");
        }
        call.status = VideoCallStatus.SCHEDULED;

        Lead lead = call.lead;
        String vendorBiz  = lead.vendor != null ? lead.vendor.businessName : "your vendor";
        String coupleName = lead.couple != null
                ? (lead.couple.partner1Name + (lead.couple.partner2Name != null ? " & " + lead.couple.partner2Name : ""))
                : "the couple";
        boolean acceptedByVendor = lead.vendor != null && lead.vendor.user != null
                && lead.vendor.user.id.equals(requestingUserId);

        // Notify the proposer that their request was accepted
        if (lead.couple != null && lead.couple.user != null
                && !lead.couple.user.id.equals(requestingUserId)) {
            notificationService.create(lead.couple.user.id, "VIDEO_CALL_ACCEPTED",
                    "Video Call Confirmed",
                    vendorBiz + " accepted the video call. It's now confirmed!", call.id);
        }
        if (lead.vendor != null && lead.vendor.user != null
                && !lead.vendor.user.id.equals(requestingUserId)) {
            notificationService.create(lead.vendor.user.id, "VIDEO_CALL_ACCEPTED",
                    "Video Call Confirmed",
                    coupleName + " accepted the video call. It's now confirmed!", call.id);
        }
        return call;
    }

    @Transactional
    public VideoCall cancel(UUID callId, UUID requestingUserId) {
        VideoCall call = findById(callId);
        if (call.status == VideoCallStatus.COMPLETED || call.status == VideoCallStatus.CANCELLED) {
            throw new BusinessException("Call cannot be cancelled in its current state");
        }
        call.status = VideoCallStatus.CANCELLED;

        Lead lead = call.lead;
        String vendorBiz = lead.vendor != null ? lead.vendor.businessName : "your vendor";
        String coupleName = lead.couple != null
                ? (lead.couple.partner1Name + (lead.couple.partner2Name != null ? " & " + lead.couple.partner2Name : ""))
                : "the couple";

        if (lead.couple != null && lead.couple.user != null
                && !lead.couple.user.id.equals(requestingUserId)) {
            notificationService.create(lead.couple.user.id, "VIDEO_CALL_CANCELLED",
                    "Video Call Cancelled", vendorBiz + " cancelled the scheduled video call.", call.id);
        }
        if (lead.vendor != null && lead.vendor.user != null
                && !lead.vendor.user.id.equals(requestingUserId)) {
            notificationService.create(lead.vendor.user.id, "VIDEO_CALL_CANCELLED",
                    "Video Call Cancelled", coupleName + " cancelled the scheduled video call.", call.id);
        }

        return call;
    }

    @Transactional
    public VideoCall reschedule(UUID callId, Instant newScheduledAt, UUID requestingUserId, boolean proposedByVendor) {
        VideoCall call = findById(callId);
        if (call.status == VideoCallStatus.COMPLETED || call.status == VideoCallStatus.CANCELLED) {
            throw new BusinessException("Call cannot be rescheduled in its current state");
        }
        call.scheduledAt = newScheduledAt;
        call.status      = VideoCallStatus.PENDING;
        call.proposedBy  = proposedByVendor ? "VENDOR" : "COUPLE";

        Lead lead = call.lead;
        String vendorBiz  = lead.vendor != null ? lead.vendor.businessName : "your vendor";
        String coupleName = lead.couple != null
                ? (lead.couple.partner1Name + (lead.couple.partner2Name != null ? " & " + lead.couple.partner2Name : ""))
                : "the couple";

        if (lead.couple != null && lead.couple.user != null
                && !lead.couple.user.id.equals(requestingUserId)) {
            notificationService.create(lead.couple.user.id, "VIDEO_CALL_RESCHEDULED",
                    "New Time Proposed", vendorBiz + " proposed a new time. Please confirm.", call.id);
        }
        if (lead.vendor != null && lead.vendor.user != null
                && !lead.vendor.user.id.equals(requestingUserId)) {
            notificationService.create(lead.vendor.user.id, "VIDEO_CALL_RESCHEDULED",
                    "New Time Proposed", coupleName + " proposed a new time. Please confirm.", call.id);
        }

        return call;
    }

    @Transactional
    public VideoCall start(UUID callId) {
        VideoCall call  = findById(callId);
        call.status     = VideoCallStatus.IN_PROGRESS;
        call.startedAt  = Instant.now();
        return call;
    }

    @Transactional
    public VideoCall end(UUID callId) {
        VideoCall call = findById(callId);
        call.status    = VideoCallStatus.COMPLETED;
        call.endedAt   = Instant.now();
        return call;
    }

    private VideoCall findById(UUID id) {
        VideoCall call = videoCallRepository.findByIdFetched(id);
        if (call == null) throw new BusinessException("Video call not found");
        return call;
    }
}

package ro.eternelle.videocall;

public enum VideoCallStatus {
    /** Proposed by one party, waiting for the other to accept/reschedule/decline. */
    PENDING,
    SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
}

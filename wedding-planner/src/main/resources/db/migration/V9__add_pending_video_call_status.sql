-- ============================================================
-- V9__add_pending_video_call_status.sql
-- Extends the video_calls status CHECK constraint to include PENDING.
-- ============================================================

ALTER TABLE video_calls DROP CHECK chk_vc_status;

ALTER TABLE video_calls
    ADD CONSTRAINT chk_vc_status
        CHECK (status IN ('PENDING','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED'));

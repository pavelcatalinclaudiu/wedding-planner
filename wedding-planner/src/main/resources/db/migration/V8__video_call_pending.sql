-- ============================================================
-- V8__video_call_pending.sql
-- Adds PENDING status support and proposed_by tracking for
-- the two-way video call confirmation flow.
-- ============================================================

-- Who initiated / last proposed this call ("COUPLE" or "VENDOR")
ALTER TABLE video_calls
    ADD COLUMN proposed_by VARCHAR(10) NULL AFTER status;

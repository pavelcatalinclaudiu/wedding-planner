-- Add moderation status to reviews table.
-- Existing reviews (already live/public) default to APPROVED.
-- New reviews created by couples will be set to PENDING by application code.

ALTER TABLE reviews
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Grandfather all existing reviews as already approved
UPDATE reviews SET status = 'APPROVED';

ALTER TABLE reviews
    ADD CONSTRAINT chk_review_status CHECK (status IN ('PENDING','APPROVED','REJECTED'));

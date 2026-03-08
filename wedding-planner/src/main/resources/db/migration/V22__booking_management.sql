-- Add status, proposed_date, proposed_note to bookings for cancel / reschedule workflow
ALTER TABLE bookings
    ADD COLUMN status        VARCHAR(30)  NOT NULL DEFAULT 'CONFIRMED',
    ADD COLUMN proposed_date DATE         NULL,
    ADD COLUMN proposed_note VARCHAR(500) NULL;

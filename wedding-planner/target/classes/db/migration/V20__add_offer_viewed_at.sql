-- Track when the couple first viewed a pending offer
ALTER TABLE offers
    ADD COLUMN viewed_at DATETIME(6) NULL DEFAULT NULL AFTER status;

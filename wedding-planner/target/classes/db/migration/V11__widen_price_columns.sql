-- ============================================================
-- V11__widen_price_columns.sql
-- Widen monetary DECIMAL(10,2) columns to DECIMAL(12,2)
-- to support values up to 9,999,999,999.99
-- ============================================================

ALTER TABLE offers
    MODIFY COLUMN price DECIMAL(12,2) NOT NULL;

ALTER TABLE leads
    MODIFY COLUMN budget DECIMAL(12,2);

ALTER TABLE bookings
    MODIFY COLUMN total_price DECIMAL(12,2) NOT NULL,
    MODIFY COLUMN deposit_paid DECIMAL(12,2) NOT NULL DEFAULT 0;

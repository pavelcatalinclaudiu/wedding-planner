ALTER TABLE vendor_profiles
    ADD COLUMN monetization_enabled TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE couple_profiles
    ADD COLUMN monetization_enabled TINYINT(1) NOT NULL DEFAULT 0;

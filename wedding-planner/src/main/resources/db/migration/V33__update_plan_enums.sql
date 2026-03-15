-- Update VendorTier enum: consolidate STARTER + PROFESSIONAL → STANDARD, keep FREE + PREMIUM
ALTER TABLE vendor_profiles DROP CHECK chk_vendor_tier;
UPDATE vendor_profiles SET tier = 'STANDARD' WHERE tier IN ('STARTER', 'PROFESSIONAL');
ALTER TABLE vendor_profiles ADD CONSTRAINT chk_vendor_tier
    CHECK (tier IN ('FREE', 'STANDARD', 'PREMIUM'));

-- Update CouplePlan enum: rename PREMIUM → DREAM_WEDDING
ALTER TABLE couple_profiles DROP CHECK chk_couple_plan;
UPDATE couple_profiles SET plan = 'DREAM_WEDDING' WHERE plan = 'PREMIUM';
ALTER TABLE couple_profiles ADD CONSTRAINT chk_couple_plan
    CHECK (plan IN ('FREE', 'DREAM_WEDDING'));

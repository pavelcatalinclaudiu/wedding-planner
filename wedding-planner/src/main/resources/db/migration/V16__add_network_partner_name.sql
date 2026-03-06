-- Make partner_id nullable to support name-only (off-platform) partners
ALTER TABLE vendor_partners MODIFY COLUMN partner_id VARCHAR(36) NULL;

-- Store the custom display name for partners not on the platform
ALTER TABLE vendor_partners ADD COLUMN partner_name VARCHAR(255) NULL;

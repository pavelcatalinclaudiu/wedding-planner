-- Add flag to track whether the couple has already used their one-time subdomain rename
ALTER TABLE couple_profiles
    ADD COLUMN subdomain_customized BOOLEAN NOT NULL DEFAULT FALSE;

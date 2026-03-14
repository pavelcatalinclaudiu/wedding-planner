-- Add profile picture to couple profiles
ALTER TABLE couple_profiles
    ADD COLUMN profile_picture VARCHAR(512) NULL DEFAULT NULL;

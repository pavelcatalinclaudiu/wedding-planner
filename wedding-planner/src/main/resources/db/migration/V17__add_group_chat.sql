-- Group planning chat: add thread type discriminator and couple reference
ALTER TABLE threads
    ADD COLUMN thread_type VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    ADD COLUMN couple_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_thread_couple
        FOREIGN KEY (couple_id) REFERENCES couple_profiles(id);

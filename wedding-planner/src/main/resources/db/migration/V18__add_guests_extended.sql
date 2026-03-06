-- Extend the guests table with full smart guest list support

ALTER TABLE guests
    DROP CHECK chk_guests_rsvp;

ALTER TABLE guests
    ADD COLUMN side             VARCHAR(20)  NOT NULL DEFAULT 'BOTH'  AFTER phone,
    ADD COLUMN guest_group      VARCHAR(20)  NOT NULL DEFAULT 'OTHER' AFTER side,
    ADD COLUMN plus_one_name    VARCHAR(200) NULL                     AFTER plus_one,
    ADD COLUMN dietary          VARCHAR(30)  NOT NULL DEFAULT 'NONE'  AFTER dietary_requirements,
    ADD COLUMN dietary_notes    VARCHAR(255) NULL,
    ADD COLUMN table_assignment VARCHAR(100) NULL,
    ADD COLUMN song_request     VARCHAR(255) NULL,
    ADD COLUMN notes            TEXT         NULL,
    ADD COLUMN is_child_guest   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN invited_by_id    VARCHAR(36)  NULL,
    ADD COLUMN updated_at       DATETIME(6)  NULL,
    ADD CONSTRAINT fk_guest_invitedby
        FOREIGN KEY (invited_by_id) REFERENCES guests(id) ON DELETE SET NULL;

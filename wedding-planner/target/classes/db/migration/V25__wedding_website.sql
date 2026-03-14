-- Wedding website content table (separate from couple_profiles)
CREATE TABLE wedding_websites (
    id                  CHAR(36)      NOT NULL PRIMARY KEY,
    couple_id           CHAR(36)      NOT NULL,
    hero_message        VARCHAR(500)  NULL,
    cover_photo_url     VARCHAR(1000) NULL,
    story               TEXT          NULL,
    ceremony_date       VARCHAR(100)  NULL,
    ceremony_location   VARCHAR(500)  NULL,
    reception_date      VARCHAR(100)  NULL,
    reception_location  VARCHAR(500)  NULL,
    is_published        TINYINT(1)    NOT NULL DEFAULT 0,
    rsvps_submitted     INT           NOT NULL DEFAULT 0,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ww_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE
);

CREATE TABLE page_visits (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    ip_hash    VARCHAR(64),
    visited_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

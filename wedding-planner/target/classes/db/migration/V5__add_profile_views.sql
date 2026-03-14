-- ============================================================
-- V5__add_profile_views.sql  –  Profile view tracking table
-- ============================================================

CREATE TABLE profile_views (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    vendor_id  VARCHAR(36)  NOT NULL,
    viewer_id  VARCHAR(36),
    ip_hash    VARCHAR(64),
    viewed_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pv_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_pv_viewer FOREIGN KEY (viewer_id) REFERENCES users(id)           ON DELETE SET NULL
);

CREATE INDEX idx_profile_views_vendor_date   ON profile_views (vendor_id, viewed_at);
CREATE INDEX idx_profile_views_dedup         ON profile_views (vendor_id, viewer_id, ip_hash, viewed_at);

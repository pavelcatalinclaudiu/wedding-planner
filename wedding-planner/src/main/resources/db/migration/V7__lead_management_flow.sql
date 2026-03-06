-- ============================================================
-- V7__lead_management_flow.sql
-- Replaces Deal/Quote pipeline with Lead → Conversation → Offer → Booking flow
-- ============================================================

-- ── NEW: LEADS ────────────────────────────────────────────────────────────────

CREATE TABLE leads (
    id          VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    couple_id   VARCHAR(36)   NOT NULL,
    vendor_id   VARCHAR(36)   NOT NULL,
    event_date  DATE,
    budget      DECIMAL(10,2),
    message     TEXT,
    status      VARCHAR(30)   NOT NULL DEFAULT 'NEW',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_leads_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_leads_status CHECK (status IN ('NEW','VIEWED','IN_DISCUSSION','QUOTED','BOOKED','DECLINED'))
);

CREATE INDEX idx_leads_couple_id ON leads (couple_id);
CREATE INDEX idx_leads_vendor_id ON leads (vendor_id);
CREATE INDEX idx_leads_status    ON leads (status);
CREATE INDEX idx_leads_created_at ON leads (created_at);

-- ── NEW: CONVERSATIONS ────────────────────────────────────────────────────────

CREATE TABLE conversations (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    lead_id         VARCHAR(36)  NOT NULL UNIQUE,
    couple_id       VARCHAR(36)  NOT NULL,
    vendor_id       VARCHAR(36)  NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_lead   FOREIGN KEY (lead_id)   REFERENCES leads(id)            ON DELETE CASCADE,
    CONSTRAINT fk_conv_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id)  ON DELETE CASCADE,
    CONSTRAINT fk_conv_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id)  ON DELETE CASCADE
);

CREATE INDEX idx_conv_lead_id   ON conversations (lead_id);
CREATE INDEX idx_conv_couple_id ON conversations (couple_id);
CREATE INDEX idx_conv_vendor_id ON conversations (vendor_id);

-- ── NEW: CONVERSATION MESSAGES ────────────────────────────────────────────────

CREATE TABLE conversation_messages (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    conversation_id VARCHAR(36)  NOT NULL,
    sender_id       VARCHAR(36),
    sender_role     VARCHAR(10)  NOT NULL,
    content         TEXT         NOT NULL,
    sent_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cmsg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cmsg_sender       FOREIGN KEY (sender_id)       REFERENCES users(id)          ON DELETE SET NULL,
    CONSTRAINT chk_cmsg_role        CHECK (sender_role IN ('COUPLE', 'VENDOR'))
);

CREATE INDEX idx_cmsg_conversation_id ON conversation_messages (conversation_id);
CREATE INDEX idx_cmsg_sent_at         ON conversation_messages (sent_at);

-- ── NEW: OFFERS ───────────────────────────────────────────────────────────────

CREATE TABLE offers (
    id              VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    lead_id         VARCHAR(36)   NOT NULL,
    conversation_id VARCHAR(36),
    vendor_id       VARCHAR(36)   NOT NULL,
    package_details TEXT,
    price           DECIMAL(10,2) NOT NULL,
    expires_at      DATE,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offers_lead         FOREIGN KEY (lead_id)         REFERENCES leads(id)           ON DELETE CASCADE,
    CONSTRAINT fk_offers_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id)   ON DELETE SET NULL,
    CONSTRAINT fk_offers_vendor       FOREIGN KEY (vendor_id)       REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_offers_status      CHECK (status IN ('PENDING','ACCEPTED','DECLINED','REVISED'))
);

CREATE INDEX idx_offers_lead_id   ON offers (lead_id);
CREATE INDEX idx_offers_vendor_id ON offers (vendor_id);
CREATE INDEX idx_offers_status    ON offers (status);

-- ── ALTER: BOOKINGS ───────────────────────────────────────────────────────────
-- Add new columns (nullable during migration)
ALTER TABLE bookings
    ADD COLUMN lead_id  VARCHAR(36) AFTER id,
    ADD COLUMN offer_id VARCHAR(36) AFTER lead_id;

-- Drop old foreign keys
ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_deal;
ALTER TABLE bookings DROP FOREIGN KEY fk_bookings_quote;

-- Drop old columns
ALTER TABLE bookings
    DROP COLUMN deal_id,
    DROP COLUMN quote_id;

-- Add new foreign keys
ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_lead  FOREIGN KEY (lead_id)  REFERENCES leads(id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_bookings_offer FOREIGN KEY (offer_id) REFERENCES offers(id) ON DELETE SET NULL;

CREATE INDEX idx_bookings_lead_id ON bookings (lead_id);

-- ── ALTER: THREADS ────────────────────────────────────────────────────────────
ALTER TABLE threads
    ADD COLUMN lead_id VARCHAR(36) AFTER id;

ALTER TABLE threads DROP FOREIGN KEY fk_threads_deal;
ALTER TABLE threads DROP INDEX     deal_id;
ALTER TABLE threads DROP COLUMN    deal_id;

ALTER TABLE threads
    ADD CONSTRAINT fk_threads_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;

CREATE INDEX idx_threads_lead_id ON threads (lead_id);

-- ── ALTER: VIDEO_CALLS ────────────────────────────────────────────────────────
ALTER TABLE video_calls
    ADD COLUMN lead_id VARCHAR(36) AFTER id;

ALTER TABLE video_calls DROP FOREIGN KEY fk_vc_deal;
ALTER TABLE video_calls DROP COLUMN     deal_id;

ALTER TABLE video_calls
    ADD CONSTRAINT fk_vc_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;

CREATE INDEX idx_video_calls_lead_id ON video_calls (lead_id);

-- ── DROP: OLD DEAL/QUOTE TABLES ───────────────────────────────────────────────
-- Must drop in FK-dependency order
DROP TABLE IF EXISTS deal_status_history;
DROP TABLE IF EXISTS quotes;
DROP TABLE IF EXISTS deals;

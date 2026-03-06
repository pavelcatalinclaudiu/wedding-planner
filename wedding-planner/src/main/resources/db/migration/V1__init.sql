-- ============================================================
-- V1__init.sql  –  Wedding Planner initial schema  (MySQL 8)
-- ============================================================

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id             VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  TEXT         NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     DATETIME,
    CONSTRAINT chk_users_role CHECK (role IN ('COUPLE','VENDOR'))
);

CREATE INDEX idx_users_email ON users (email);

-- ── COUPLE PROFILES ──────────────────────────────────────────
CREATE TABLE couple_profiles (
    id                 VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    user_id            VARCHAR(36)   NOT NULL UNIQUE,
    partner_1_name     VARCHAR(120),
    partner_2_name     VARCHAR(120),
    wedding_date       DATE,
    wedding_location   VARCHAR(255),
    guest_count        INT           NOT NULL DEFAULT 0,
    total_budget       DECIMAL(10,2) NOT NULL DEFAULT 0,
    plan               VARCHAR(20)   NOT NULL DEFAULT 'FREE',
    website_subdomain  VARCHAR(80)   UNIQUE,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_couple_user  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_couple_plan CHECK (plan IN ('FREE','PREMIUM'))
);

CREATE INDEX idx_couple_user_id ON couple_profiles (user_id);

-- ── VENDOR PROFILES ──────────────────────────────────────────
CREATE TABLE vendor_profiles (
    id             VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    user_id        VARCHAR(36)   NOT NULL UNIQUE,
    business_name  VARCHAR(200)  NOT NULL,
    category       VARCHAR(50)   NOT NULL,
    city           VARCHAR(120)  NOT NULL,
    description    TEXT,
    base_price     DECIMAL(10,2),
    tier           VARCHAR(20)   NOT NULL DEFAULT 'FREE',
    avg_rating     DECIMAL(3,2)  NOT NULL DEFAULT 0,
    review_count   INT           NOT NULL DEFAULT 0,
    profile_views  INT           NOT NULL DEFAULT 0,
    is_verified    BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendor_user  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_vendor_tier CHECK (tier IN ('FREE','STARTER','PROFESSIONAL','PREMIUM'))
);

CREATE INDEX idx_vendor_user_id   ON vendor_profiles (user_id);
CREATE INDEX idx_vendor_category  ON vendor_profiles (category);
CREATE INDEX idx_vendor_city      ON vendor_profiles (city);
CREATE INDEX idx_vendor_tier      ON vendor_profiles (tier);
CREATE INDEX idx_vendor_rating    ON vendor_profiles (avg_rating DESC);

-- ── VENDOR PHOTOS ─────────────────────────────────────────────
CREATE TABLE vendor_photos (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    vendor_id   VARCHAR(36)  NOT NULL,
    url         TEXT         NOT NULL,
    caption     VARCHAR(255),
    is_cover    BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendor_photos_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_photos_vendor_id ON vendor_photos (vendor_id);

-- ── DEALS ────────────────────────────────────────────────────
CREATE TABLE deals (
    id             VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    couple_id      VARCHAR(36)  NOT NULL,
    vendor_id      VARCHAR(36)  NOT NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'ENQUIRY',
    sealed_at      DATETIME,
    sealed_trigger VARCHAR(30),
    video_call_id  VARCHAR(36),
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (couple_id, vendor_id),
    CONSTRAINT fk_deals_couple   FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_deals_vendor   FOREIGN KEY (vendor_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_deals_status  CHECK (status IN ('ENQUIRY','CONVERSATION','QUOTE_SENT','QUOTE_ACCEPTED','DEPOSIT_PAID','CONFIRMED')),
    CONSTRAINT chk_deals_trigger CHECK (sealed_trigger IN ('DEPOSIT_PAID','VENDOR_CONFIRMED','MANUAL'))
);

CREATE INDEX idx_deals_couple_id ON deals (couple_id);
CREATE INDEX idx_deals_vendor_id ON deals (vendor_id);
CREATE INDEX idx_deals_status    ON deals (status);

-- ── DEAL STATUS HISTORY ───────────────────────────────────────
CREATE TABLE deal_status_history (
    id           VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    deal_id      VARCHAR(36)  NOT NULL,
    from_status  VARCHAR(30),
    to_status    VARCHAR(30)  NOT NULL,
    actor_id     VARCHAR(36),
    note         TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deal_history_deal  FOREIGN KEY (deal_id)  REFERENCES deals(id) ON DELETE CASCADE,
    CONSTRAINT fk_deal_history_actor FOREIGN KEY (actor_id) REFERENCES users(id)  ON DELETE SET NULL
);

CREATE INDEX idx_deal_history_deal_id ON deal_status_history (deal_id);

-- ── QUOTES ───────────────────────────────────────────────────
CREATE TABLE quotes (
    id             VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    deal_id        VARCHAR(36)   NOT NULL,
    package_name   VARCHAR(200)  NOT NULL,
    total_price    DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2),
    items          JSON          NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    expires_at     DATETIME,
    accepted_at    DATETIME,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quotes_deal    FOREIGN KEY (deal_id) REFERENCES deals(id) ON DELETE CASCADE,
    CONSTRAINT chk_quotes_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED'))
);

CREATE INDEX idx_quotes_deal_id ON quotes (deal_id);
CREATE INDEX idx_quotes_status  ON quotes (status);

-- ── BOOKINGS ─────────────────────────────────────────────────
CREATE TABLE bookings (
    id               VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    deal_id          VARCHAR(36)   NOT NULL UNIQUE,
    quote_id         VARCHAR(36)   UNIQUE,
    wedding_date     DATE          NOT NULL,
    total_price      DECIMAL(10,2) NOT NULL,
    deposit_paid     DECIMAL(10,2) NOT NULL DEFAULT 0,
    deposit_paid_at  DATETIME,
    balance_due_at   DATE,
    contract_url     TEXT,
    notes            TEXT,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_deal  FOREIGN KEY (deal_id)  REFERENCES deals(id)  ON DELETE CASCADE,
    CONSTRAINT fk_bookings_quote FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE SET NULL
);

CREATE INDEX idx_bookings_deal_id ON bookings (deal_id);

-- ── MESSAGING: THREADS ───────────────────────────────────────
CREATE TABLE threads (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    deal_id         VARCHAR(36)  UNIQUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_threads_deal FOREIGN KEY (deal_id) REFERENCES deals(id) ON DELETE CASCADE
);

CREATE INDEX idx_threads_deal_id ON threads (deal_id);

-- ── MESSAGING: THREAD PARTICIPANTS ───────────────────────────
CREATE TABLE thread_participants (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    thread_id  VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    joined_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (thread_id, user_id),
    CONSTRAINT fk_tp_thread FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    CONSTRAINT fk_tp_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
);

CREATE INDEX idx_thread_participants_thread_id ON thread_participants (thread_id);
CREATE INDEX idx_thread_participants_user_id   ON thread_participants (user_id);

-- ── MESSAGING: MESSAGES ──────────────────────────────────────
CREATE TABLE messages (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    thread_id  VARCHAR(36)  NOT NULL,
    sender_id  VARCHAR(36),
    content    TEXT         NOT NULL,
    type       VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    read_by    JSON         NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)   ON DELETE SET NULL,
    CONSTRAINT chk_messages_type  CHECK (type IN ('TEXT','IMAGE','FILE','SYSTEM'))
);

CREATE INDEX idx_messages_thread_id  ON messages (thread_id);
CREATE INDEX idx_messages_created_at ON messages (created_at);

-- ── VIDEO CALLS ──────────────────────────────────────────────
CREATE TABLE video_calls (
    id                VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    deal_id           VARCHAR(36)  NOT NULL,
    room_url          TEXT,
    room_name         VARCHAR(100),
    scheduled_at      DATETIME,
    started_at        DATETIME,
    ended_at          DATETIME,
    status            VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    post_call_action  VARCHAR(30),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vc_deal         FOREIGN KEY (deal_id) REFERENCES deals(id) ON DELETE CASCADE,
    CONSTRAINT chk_vc_status      CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_vc_post_action CHECK (post_call_action IN ('SEND_QUOTE','CONFIRM_DEAL','SCHEDULE_FOLLOW_UP','NONE'))
);

CREATE INDEX idx_video_calls_deal_id      ON video_calls (deal_id);
CREATE INDEX idx_video_calls_scheduled_at ON video_calls (scheduled_at);

-- ── NOTIFICATIONS ────────────────────────────────────────────
CREATE TABLE notifications (
    id         VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    user_id    VARCHAR(36)   NOT NULL,
    type       VARCHAR(60)   NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    body       TEXT,
    entity_id  VARCHAR(36),
    read_flag  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_unread  ON notifications (user_id, read_flag);

-- ── REVIEWS ──────────────────────────────────────────────────
CREATE TABLE reviews (
    id          VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    booking_id  VARCHAR(36)   NOT NULL UNIQUE,
    couple_id   VARCHAR(36)   NOT NULL,
    vendor_id   VARCHAR(36)   NOT NULL,
    rating      DECIMAL(2,1)  NOT NULL,
    comment     TEXT,
    is_public   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)        ON DELETE CASCADE,
    CONSTRAINT fk_reviews_couple  FOREIGN KEY (couple_id)  REFERENCES couple_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_vendor  FOREIGN KEY (vendor_id)  REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_vendor_id ON reviews (vendor_id);
CREATE INDEX idx_reviews_couple_id ON reviews (couple_id);

-- ── GUESTS ───────────────────────────────────────────────────
CREATE TABLE guests (
    id                   VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    couple_id            VARCHAR(36)   NOT NULL,
    first_name           VARCHAR(100)  NOT NULL,
    last_name            VARCHAR(100),
    email                VARCHAR(255),
    phone                VARCHAR(30),
    rsvp_status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    plus_one             BOOLEAN       NOT NULL DEFAULT FALSE,
    dietary_requirements TEXT,
    table_number         VARCHAR(20),
    invite_sent          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guests_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_guests_rsvp  CHECK (rsvp_status IN ('PENDING','CONFIRMED','DECLINED'))
);

CREATE INDEX idx_guests_couple_id ON guests (couple_id);

-- ── BUDGET ITEMS ─────────────────────────────────────────────
CREATE TABLE budget_items (
    id              VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    couple_id       VARCHAR(36)   NOT NULL,
    category        VARCHAR(100)  NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    estimated_cost  DECIMAL(10,2) NOT NULL DEFAULT 0,
    actual_cost     DECIMAL(10,2),
    is_paid         BOOLEAN       NOT NULL DEFAULT FALSE,
    vendor_name     VARCHAR(200),
    notes           TEXT,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_budget_items_couple_id ON budget_items (couple_id);

-- ── CHECKLIST ITEMS ──────────────────────────────────────────
CREATE TABLE checklist_items (
    id           VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    couple_id    VARCHAR(36)   NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    description  TEXT,
    category     VARCHAR(100),
    due_date     DATE,
    completed    BOOLEAN       NOT NULL DEFAULT FALSE,
    completed_at DATETIME,
    sort_order   INT           NOT NULL DEFAULT 0,
    is_default   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_checklist_couple FOREIGN KEY (couple_id) REFERENCES couple_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_checklist_items_couple_id ON checklist_items (couple_id);

-- ── VENDOR PARTNERS ──────────────────────────────────────────
CREATE TABLE vendor_partners (
    id                VARCHAR(36)  PRIMARY KEY DEFAULT (UUID()),
    vendor_id         VARCHAR(36)  NOT NULL,
    partner_id        VARCHAR(36)  NOT NULL,
    partnership_note  TEXT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (vendor_id, partner_id),
    CONSTRAINT fk_vp_vendor  FOREIGN KEY (vendor_id)  REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_vp_partner FOREIGN KEY (partner_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_partners_vendor_id ON vendor_partners (vendor_id);

-- ── SUBSCRIPTIONS ────────────────────────────────────────────
CREATE TABLE subscriptions (
    id                      VARCHAR(36)   PRIMARY KEY DEFAULT (UUID()),
    user_id                 VARCHAR(36)   NOT NULL,
    stripe_subscription_id  VARCHAR(200)  UNIQUE,
    stripe_customer_id      VARCHAR(200),
    stripe_price_id         VARCHAR(200),
    plan_name               VARCHAR(100)  NOT NULL,
    status                  VARCHAR(30)   NOT NULL,
    current_period_start    DATETIME,
    current_period_end      DATETIME,
    cancel_at_period_end    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_status  ON subscriptions (status);

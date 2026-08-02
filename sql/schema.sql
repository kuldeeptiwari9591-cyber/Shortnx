-- ShortNx schema — run this in Supabase SQL editor

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(60)  NOT NULL,     -- bcrypt hash is always 60 chars
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE links (
    id          BIGSERIAL PRIMARY KEY,
    short_code  VARCHAR(20)  UNIQUE NOT NULL,
    long_url    TEXT         NOT NULL,
    user_id     BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    expiry      TIMESTAMPTZ  NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_links_short_code ON links(short_code);
CREATE INDEX idx_links_user_id ON links(user_id);

CREATE TABLE clicks (
    id          BIGSERIAL PRIMARY KEY,
    link_id     BIGINT REFERENCES links(id) ON DELETE CASCADE,
    clicked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_hash     VARCHAR(64),      -- store a hash, not the raw IP (privacy)
    user_agent  TEXT,
    referrer    TEXT
);

CREATE INDEX idx_clicks_link_id ON clicks(link_id);

-- Basic rate-limit tracking table (used by RateLimiter util as a fallback
-- if you don't want to add Redis for a resume project)
CREATE TABLE request_log (
    ip_hash     VARCHAR(64) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    hits        INT NOT NULL DEFAULT 1,
    PRIMARY KEY (ip_hash, window_start)
);

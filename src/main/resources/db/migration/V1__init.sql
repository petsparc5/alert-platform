CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    role          VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    api_key_hash  VARCHAR(64) NOT NULL UNIQUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE channel_configs (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL CHECK (type IN ('EMAIL', 'SLACK_WEBHOOK')),
    config      JSONB NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_channel_configs_user_id ON channel_configs (user_id);

CREATE TABLE alert_rules (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category    VARCHAR(30) NOT NULL CHECK (category IN ('BREAKING_NEWS', 'MARKET', 'DISASTER')),
    criteria    JSONB NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alert_rules_user_id ON alert_rules (user_id);
CREATE INDEX idx_alert_rules_active_category ON alert_rules (active, category);

CREATE TABLE events (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dedup_key    VARCHAR(255) NOT NULL UNIQUE,
    category     VARCHAR(30) NOT NULL CHECK (category IN ('BREAKING_NEWS', 'MARKET', 'DISASTER')),
    type         VARCHAR(50) NOT NULL,
    source       VARCHAR(50) NOT NULL,
    severity     DOUBLE PRECISION,
    occurred_at  TIMESTAMPTZ NOT NULL,
    ingested_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload      JSONB NOT NULL
);

CREATE INDEX idx_events_category ON events (category);
CREATE INDEX idx_events_occurred_at ON events (occurred_at);

CREATE TABLE notification_deliveries (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel_type  VARCHAR(30) NOT NULL CHECK (channel_type IN ('EMAIL', 'SLACK_WEBHOOK')),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DEAD_LETTERED')),
    attempts      INT NOT NULL DEFAULT 0,
    error         TEXT,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_deliveries_event_id ON notification_deliveries (event_id);
CREATE INDEX idx_notification_deliveries_user_id ON notification_deliveries (user_id);

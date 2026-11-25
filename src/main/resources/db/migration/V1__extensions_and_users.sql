CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username          CITEXT NOT NULL UNIQUE CHECK (username ~ '^[a-z0-9_.]{3,30}$'),
  email             CITEXT NOT NULL UNIQUE,
  email_verified_at TIMESTAMPTZ,
  password_hash     TEXT NOT NULL,
  first_name        VARCHAR(50) NOT NULL,
  last_name         VARCHAR(80),
  birth_date        DATE NOT NULL,
  avatar_key        TEXT,
  bio               VARCHAR(160),
  is_private        BOOLEAN NOT NULL DEFAULT false,
  status            VARCHAR(20) NOT NULL DEFAULT 'pending'
                    CHECK (status IN ('pending','active','suspended','deleted')),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ
);

CREATE TABLE user_2fa (
  user_id    UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  secret_enc BYTEA NOT NULL,
  enabled_at TIMESTAMPTZ
);

CREATE TABLE user_settings (
  user_id     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  notif_prefs JSONB NOT NULL DEFAULT '{}',
  language    VARCHAR(5) NOT NULL DEFAULT 'es'
);

CREATE TABLE user_devices (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  platform     VARCHAR(10) NOT NULL CHECK (platform IN ('android','ios')),
  device_name  VARCHAR(100),
  app_version  VARCHAR(20),
  push_token   TEXT UNIQUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_user_devices_user ON user_devices (user_id);

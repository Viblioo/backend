CREATE TABLE category (
  id   SMALLSERIAL PRIMARY KEY,
  slug VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE event (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id         UUID NOT NULL REFERENCES users(id),
  title            VARCHAR(100) NOT NULL,
  description      TEXT,
  category_id      SMALLINT REFERENCES category(id),
  place_name       VARCHAR(150),
  lat              NUMERIC(9,6),
  lon              NUMERIC(9,6),
  starts_at        TIMESTAMPTZ NOT NULL,
  ends_at          TIMESTAMPTZ,
  visibility       VARCHAR(10) NOT NULL CHECK (visibility IN ('private','friends','public')),
  max_participants INT CHECK (max_participants IS NULL OR max_participants >= 2),
  cover_key        TEXT,
  currency         CHAR(3) NOT NULL DEFAULT 'EUR',
  status           VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (status IN ('active','cancelled')),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (ends_at IS NULL OR ends_at >= starts_at)
);
CREATE INDEX idx_event_owner ON event (owner_id);
CREATE INDEX idx_event_public_upcoming ON event (starts_at)
  WHERE status = 'active' AND visibility = 'public';

CREATE TABLE event_participant (
  event_id             UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role                 VARCHAR(10) NOT NULL DEFAULT 'member' CHECK (role IN ('owner','admin','member')),
  status               VARCHAR(10) NOT NULL
                       CHECK (status IN ('invited','requested','accepted','declined','left')),
  invited_by           UUID REFERENCES users(id) ON DELETE SET NULL,
  last_read_message_id UUID,
  chat_muted_until     TIMESTAMPTZ,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (event_id, user_id)
);
CREATE INDEX idx_event_participant_user ON event_participant (user_id, status);
CREATE UNIQUE INDEX uq_event_single_owner ON event_participant (event_id) WHERE role = 'owner';

CREATE TABLE event_interest (
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_id   UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, event_id)
);

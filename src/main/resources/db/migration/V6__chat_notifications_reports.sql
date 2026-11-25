CREATE TABLE message (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id          UUID NOT NULL REFERENCES event(id) ON DELETE CASCADE,
  sender_id         UUID REFERENCES users(id) ON DELETE SET NULL,
  body              TEXT,
  attachment_key    TEXT,
  client_message_id UUID UNIQUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  edited_at         TIMESTAMPTZ,
  deleted_at        TIMESTAMPTZ,
  CHECK (body IS NOT NULL OR attachment_key IS NOT NULL)
);
CREATE INDEX idx_message_event ON message (event_id, created_at DESC);

CREATE TABLE notification (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  to_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  from_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  type         VARCHAR(30) NOT NULL,
  entity_type  VARCHAR(20),
  entity_id    UUID,
  data         JSONB,
  read_at      TIMESTAMPTZ,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_user ON notification (to_user_id, created_at DESC);
CREATE INDEX idx_notification_unread ON notification (to_user_id) WHERE read_at IS NULL;

CREATE TABLE report (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID NOT NULL REFERENCES users(id),
  target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('user','event','photo','comment','message')),
  target_id   UUID NOT NULL,
  reason      VARCHAR(30) NOT NULL,
  details     TEXT,
  status      VARCHAR(12) NOT NULL DEFAULT 'open'
              CHECK (status IN ('open','reviewing','actioned','dismissed')),
  reviewed_by UUID REFERENCES users(id),
  reviewed_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_report_status ON report (status, created_at);

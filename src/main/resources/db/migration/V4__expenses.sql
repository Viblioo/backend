CREATE TABLE expense (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id          UUID NOT NULL REFERENCES event(id),
  payer_id          UUID NOT NULL REFERENCES users(id),
  created_by        UUID NOT NULL REFERENCES users(id),
  description       VARCHAR(120) NOT NULL,
  amount_cents      BIGINT NOT NULL CHECK (amount_cents > 0),
  split_type        VARCHAR(10) NOT NULL CHECK (split_type IN ('equal','exact','shares')),
  spent_at          DATE NOT NULL,
  client_request_id UUID UNIQUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ
);
CREATE INDEX idx_expense_event ON expense (event_id) WHERE deleted_at IS NULL;

CREATE TABLE expense_share (
  expense_id   UUID NOT NULL REFERENCES expense(id) ON DELETE CASCADE,
  user_id      UUID NOT NULL REFERENCES users(id),
  amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
  PRIMARY KEY (expense_id, user_id)
);
CREATE INDEX idx_expense_share_user ON expense_share (user_id);

CREATE TABLE settlement (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id          UUID NOT NULL REFERENCES event(id),
  from_user         UUID NOT NULL REFERENCES users(id),
  to_user           UUID NOT NULL REFERENCES users(id),
  amount_cents      BIGINT NOT NULL CHECK (amount_cents > 0),
  confirmed_at      TIMESTAMPTZ,
  client_request_id UUID UNIQUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (from_user <> to_user)
);
CREATE INDEX idx_settlement_event ON settlement (event_id);

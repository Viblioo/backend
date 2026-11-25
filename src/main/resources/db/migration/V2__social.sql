CREATE TABLE friend_request (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  receiver_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status       VARCHAR(10) NOT NULL DEFAULT 'pending'
               CHECK (status IN ('pending','accepted','rejected','cancelled')),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  responded_at TIMESTAMPTZ,
  CHECK (sender_id <> receiver_id)
);
CREATE UNIQUE INDEX uq_friend_request_pending
  ON friend_request (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
  WHERE status = 'pending';
CREATE INDEX idx_friend_request_receiver ON friend_request (receiver_id) WHERE status = 'pending';

CREATE TABLE friendship (
  user_a     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_a, user_b),
  CHECK (user_a < user_b)
);
CREATE INDEX idx_friendship_b ON friendship (user_b);

CREATE TABLE follow (
  follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  followed_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (follower_id, followed_id),
  CHECK (follower_id <> followed_id)
);
CREATE INDEX idx_follow_followed ON follow (followed_id);

CREATE TABLE user_block (
  blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (blocker_id, blocked_id),
  CHECK (blocker_id <> blocked_id)
);
CREATE INDEX idx_user_block_blocked ON user_block (blocked_id);

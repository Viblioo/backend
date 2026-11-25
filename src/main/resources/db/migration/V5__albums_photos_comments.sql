CREATE TABLE album (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id   UUID NOT NULL UNIQUE REFERENCES event(id) ON DELETE CASCADE,
  unlock_at  TIMESTAMPTZ,
  visibility VARCHAR(12) NOT NULL DEFAULT 'participants'
             CHECK (visibility IN ('participants','friends','public')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE photo (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  album_id      UUID NOT NULL REFERENCES album(id) ON DELETE CASCADE,
  uploader_id   UUID NOT NULL REFERENCES users(id),
  storage_key   TEXT NOT NULL,
  width         INT,
  height        INT,
  size_bytes    INT,
  status        VARCHAR(12) NOT NULL DEFAULT 'processing'
                CHECK (status IN ('processing','ready','rejected','deleted')),
  like_count    INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_photo_album ON photo (album_id, created_at);
CREATE INDEX idx_photo_uploader ON photo (uploader_id);

CREATE TABLE photo_like (
  photo_id   UUID NOT NULL REFERENCES photo(id) ON DELETE CASCADE,
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (photo_id, user_id)
);
CREATE INDEX idx_photo_like_user ON photo_like (user_id);

CREATE TABLE comment (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_id  UUID NOT NULL REFERENCES users(id),
  event_id   UUID REFERENCES event(id) ON DELETE CASCADE,
  photo_id   UUID REFERENCES photo(id) ON DELETE CASCADE,
  parent_id  UUID REFERENCES comment(id) ON DELETE CASCADE,
  body       VARCHAR(1000) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  edited_at  TIMESTAMPTZ,
  deleted_at TIMESTAMPTZ,
  CHECK ((event_id IS NOT NULL)::int + (photo_id IS NOT NULL)::int = 1)
);
CREATE INDEX idx_comment_event ON comment (event_id, created_at) WHERE event_id IS NOT NULL;
CREATE INDEX idx_comment_photo ON comment (photo_id, created_at) WHERE photo_id IS NOT NULL;

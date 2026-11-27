-- Categorías iniciales de eventos. El slug es el identificador estable (máx. 30 caracteres,
-- minúsculas, sin tildes); el nombre visible y su traducción los pone el frontend.
-- ON CONFLICT DO NOTHING: si alguna ya existe (p. ej. creada a mano para pruebas), no falla.

INSERT INTO category (slug) VALUES
  -- General
  ('fiesta'),

  -- Tipo de plan
  ('discoteca'),
  ('club'),
  ('rave'),
  ('festival'),
  ('concierto'),
  ('dj-set'),
  ('after'),
  ('fiesta-en-casa'),
  ('bar-de-copas'),
  ('pub'),
  ('cocteleria'),
  ('terraza'),
  ('rooftop'),
  ('tardeo'),
  ('afterwork'),
  ('ruta-de-bares'),
  ('karaoke'),
  ('cena'),
  ('brunch'),
  ('vermut'),

  -- Música electrónica
  ('techno'),
  ('house'),
  ('deep-house'),
  ('tech-house'),
  ('minimal'),
  ('trance'),
  ('hard-techno'),
  ('drum-and-bass'),
  ('dubstep'),
  ('edm'),
  ('electronica'),

  -- Música latina y urbana
  ('reggaeton'),
  ('latino'),
  ('salsa'),
  ('bachata'),
  ('merengue'),
  ('dancehall'),
  ('afrobeats'),
  ('hip-hop'),
  ('trap'),
  ('rnb'),

  -- Otros estilos
  ('pop'),
  ('indie'),
  ('rock'),
  ('punk'),
  ('metal'),
  ('disco'),
  ('funk'),
  ('soul'),
  ('jazz'),
  ('flamenco'),
  ('rumba'),
  ('k-pop'),
  ('remember-80s'),
  ('remember-90s'),
  ('remember-2000s'),
  ('musica-en-directo'),
  ('jam-session'),

  -- Temáticas y fechas especiales
  ('fiesta-tematica'),
  ('disfraces'),
  ('halloween'),
  ('nochevieja'),
  ('carnaval'),
  ('san-juan'),
  ('orgullo'),
  ('sangriadas'),
  ('erasmus'),
  ('despedida'),
  ('cumpleanos'),
  ('fiesta-de-la-espuma'),
  ('fiesta-blanca'),
  ('fiesta-neon'),

  -- Al aire libre
  ('playa'),
  ('piscina'),
  ('fiesta-en-barco'),
  ('picnic'),
  ('acampada'),

  -- Social y conocer gente
  ('quedada'),
  ('conocer-gente'),
  ('intercambio-de-idiomas'),
  ('networking'),

  -- Ocio y cultura nocturna
  ('stand-up-comedy'),
  ('open-mic'),
  ('cine-al-aire-libre'),
  ('teatro'),
  ('juegos-de-mesa'),
  ('trivia'),
  ('ver-partido'),
  ('gaming')
ON CONFLICT (slug) DO NOTHING;
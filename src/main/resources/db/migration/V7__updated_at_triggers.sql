CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_event_updated_at
  BEFORE UPDATE ON event
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_event_participant_updated_at
  BEFORE UPDATE ON event_participant
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_expense_updated_at
  BEFORE UPDATE ON expense
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

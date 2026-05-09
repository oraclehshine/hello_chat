ALTER TABLE group_members ADD COLUMN IF NOT EXISTS last_read_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_group_members_group_user_read ON group_members(group_id, user_id, last_read_at);

ALTER TABLE groups ADD COLUMN IF NOT EXISTS notice_updated_at TIMESTAMP;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS notice_read_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_group_members_notice_read ON group_members(group_id, notice_read_at);

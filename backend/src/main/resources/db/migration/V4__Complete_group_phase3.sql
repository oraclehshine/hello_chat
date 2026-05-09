ALTER TABLE groups ADD COLUMN IF NOT EXISTS invite_code VARCHAR(64);
ALTER TABLE groups ADD COLUMN IF NOT EXISTS chat_enabled SMALLINT DEFAULT 1;
ALTER TABLE groups ADD COLUMN IF NOT EXISTS recall_limit_minutes INTEGER DEFAULT 2;

CREATE UNIQUE INDEX IF NOT EXISTS idx_groups_invite_code ON groups(invite_code);
CREATE INDEX IF NOT EXISTS idx_groups_name ON groups(name);

CREATE TABLE IF NOT EXISTS group_join_requests (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    message VARCHAR(255),
    status SMALLINT DEFAULT 0,
    handled_by BIGINT,
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (requester_id) REFERENCES users(id),
    FOREIGN KEY (handled_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_group_join_requests_group_id_status ON group_join_requests(group_id, status);
CREATE INDEX IF NOT EXISTS idx_group_join_requests_requester_id ON group_join_requests(requester_id);

CREATE TABLE IF NOT EXISTS group_message_mentions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (message_id) REFERENCES group_messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_group_message_mentions_user_id ON group_message_mentions(user_id);

CREATE TABLE IF NOT EXISTS group_notifications (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    actor_id BIGINT,
    target_user_id BIGINT,
    notice_type VARCHAR(64) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (actor_id) REFERENCES users(id),
    FOREIGN KEY (target_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_group_notifications_group_id_created_at ON group_notifications(group_id, created_at DESC);

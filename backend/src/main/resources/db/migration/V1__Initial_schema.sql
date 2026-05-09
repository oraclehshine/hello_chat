-- ============================================================================
-- Hello Chat 鏁版嵁搴撳垵濮嬪寲鑴氭湰
-- 鍒涘缓鏃堕棿: 2026-05-09
-- 鐗堟湰: V1.0
-- 璇存槑: 鍖呭惈鎵€鏈夋牳蹇冭〃鍙婄储寮?
-- ============================================================================

-- 1. users 鐢ㄦ埛琛?
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(255),
    signature VARCHAR(255),
    gender SMALLINT DEFAULT 0,
    age INTEGER,
    phone VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);

-- 2. email_captchas 閭楠岃瘉鐮佽〃
CREATE TABLE email_captchas (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(128) NOT NULL,
    scene VARCHAR(32) NOT NULL,
    captcha VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    send_count INTEGER DEFAULT 1,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_email_captchas_email_scene ON email_captchas(email, scene);
CREATE INDEX idx_email_captchas_expires_at ON email_captchas(expires_at);

-- 3. friend_requests table
CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    remark VARCHAR(255),
    status SMALLINT DEFAULT 1,
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (requester_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    UNIQUE(requester_id, receiver_id)
);

CREATE INDEX idx_friend_requests_receiver_id ON friend_requests(receiver_id);
CREATE INDEX idx_friend_requests_status ON friend_requests(status);

-- 4. friendships 濂藉弸鍏崇郴琛?
CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    remark_name VARCHAR(64),
    friend_group VARCHAR(64),
    is_star SMALLINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_a_id) REFERENCES users(id),
    FOREIGN KEY (user_b_id) REFERENCES users(id),
    UNIQUE(user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
);

CREATE INDEX idx_friendships_user_a_id ON friendships(user_a_id);
CREATE INDEX idx_friendships_user_b_id ON friendships(user_b_id);

-- 5. user_blocks 榛戝悕鍗曡〃
CREATE TABLE user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (blocker_id) REFERENCES users(id),
    FOREIGN KEY (blocked_id) REFERENCES users(id),
    UNIQUE(blocker_id, blocked_id)
);

CREATE INDEX idx_user_blocks_blocker_id ON user_blocks(blocker_id);
CREATE INDEX idx_user_blocks_blocked_id ON user_blocks(blocked_id);

-- 6. file_assets 鏂囦欢璧勬簮琛?
CREATE TABLE file_assets (
    id BIGSERIAL PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    mime_type VARCHAR(128),
    file_size BIGINT NOT NULL,
    scene VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE INDEX idx_file_assets_uploader_id ON file_assets(uploader_id);
CREATE INDEX idx_file_assets_scene ON file_assets(scene);

-- 7. private_chats 鍗曡亰浼氳瘽琛?
CREATE TABLE private_chats (
    id BIGSERIAL PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    last_message_id BIGINT,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_a_id) REFERENCES users(id),
    FOREIGN KEY (user_b_id) REFERENCES users(id),
    UNIQUE(user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
);

CREATE INDEX idx_private_chats_user_a_id ON private_chats(user_a_id);
CREATE INDEX idx_private_chats_user_b_id ON private_chats(user_b_id);

-- 8. private_messages 鍗曡亰娑堟伅琛?
CREATE TABLE private_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(16) NOT NULL,
    content TEXT,
    file_id BIGINT,
    reply_to_message_id BIGINT,
    recall_status SMALLINT DEFAULT 0,
    message_status SMALLINT DEFAULT 1,
    sent_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES private_chats(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (file_id) REFERENCES file_assets(id),
    FOREIGN KEY (reply_to_message_id) REFERENCES private_messages(id)
);

CREATE INDEX idx_private_messages_chat_id_sent_at ON private_messages(chat_id, sent_at DESC);
CREATE INDEX idx_private_messages_sender_id ON private_messages(sender_id);

-- 9. groups 缇ょ粍琛?
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    avatar_url VARCHAR(255),
    notice TEXT,
    status SMALLINT DEFAULT 1,
    max_member_count INTEGER DEFAULT 500,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_groups_owner_id ON groups(owner_id);
CREATE INDEX idx_groups_status ON groups(status);

-- 10. group_members 缇ゆ垚鍛樿〃
CREATE TABLE group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role SMALLINT DEFAULT 1,
    nickname VARCHAR(64),
    mute_until TIMESTAMP,
    joined_at TIMESTAMP NOT NULL,
    left_at TIMESTAMP,
    status SMALLINT DEFAULT 1,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group_id ON group_members(group_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);

-- 11. group_messages 缇ゆ秷鎭〃
CREATE TABLE group_messages (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(16) NOT NULL,
    content TEXT,
    file_id BIGINT,
    reply_to_message_id BIGINT,
    mention_all SMALLINT DEFAULT 0,
    recall_status SMALLINT DEFAULT 0,
    sent_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (file_id) REFERENCES file_assets(id),
    FOREIGN KEY (reply_to_message_id) REFERENCES group_messages(id)
);

CREATE INDEX idx_group_messages_group_id_sent_at ON group_messages(group_id, sent_at DESC);
CREATE INDEX idx_group_messages_sender_id ON group_messages(sender_id);

-- 12. moments 鏈嬪弸鍦堝姩鎬佽〃
CREATE TABLE moments (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    content TEXT,
    location VARCHAR(128),
    visibility VARCHAR(16) NOT NULL,
    like_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    collect_count INTEGER DEFAULT 0,
    view_count INTEGER DEFAULT 0,
    edited_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_moments_author_id ON moments(author_id);
CREATE INDEX idx_moments_created_at ON moments(created_at DESC);
CREATE INDEX idx_moments_visibility ON moments(visibility);

-- 13. moment_media 鍔ㄦ€佸獟浣撹〃
CREATE TABLE moment_media (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sort_order INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (file_id) REFERENCES file_assets(id)
);

CREATE INDEX idx_moment_media_moment_id ON moment_media(moment_id);

-- 14. moment_comments 鍔ㄦ€佽瘎璁鸿〃
CREATE TABLE moment_comments (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reply_to_comment_id BIGINT,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (reply_to_comment_id) REFERENCES moment_comments(id)
);

CREATE INDEX idx_moment_comments_moment_id ON moment_comments(moment_id);
CREATE INDEX idx_moment_comments_user_id ON moment_comments(user_id);

-- 15. moment_likes 鍔ㄦ€佺偣璧炶〃
CREATE TABLE moment_likes (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(moment_id, user_id)
);

CREATE INDEX idx_moment_likes_moment_id ON moment_likes(moment_id);
CREATE INDEX idx_moment_likes_user_id ON moment_likes(user_id);

-- 16. moment_collects 鍔ㄦ€佹敹钘忚〃
CREATE TABLE moment_collects (
    id BIGSERIAL PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(moment_id, user_id)
);

CREATE INDEX idx_moment_collects_moment_id ON moment_collects(moment_id);
CREATE INDEX idx_moment_collects_user_id ON moment_collects(user_id);

-- 17. notifications 閫氱煡琛?
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    related_id BIGINT,
    is_read SMALLINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_id_created_at ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

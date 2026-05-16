CREATE TABLE IF NOT EXISTS admin_operation_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_name VARCHAR(64) NOT NULL DEFAULT '平台管理员',
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target VARCHAR(255) NOT NULL,
    ip VARCHAR(64) NOT NULL DEFAULT '127.0.0.1',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_operation_logs_created_at
    ON admin_operation_logs (created_at DESC);

CREATE TABLE IF NOT EXISTS sensitive_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(128) NOT NULL UNIQUE,
    scene VARCHAR(32) NOT NULL DEFAULT 'chat',
    level VARCHAR(16) NOT NULL DEFAULT 'medium',
    action VARCHAR(16) NOT NULL DEFAULT 'warn',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    hit_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sensitive_words_scene CHECK (scene IN ('chat', 'moment', 'group', 'announcement', 'all')),
    CONSTRAINT ck_sensitive_words_level CHECK (level IN ('low', 'medium', 'high')),
    CONSTRAINT ck_sensitive_words_action CHECK (action IN ('warn', 'block', 'review'))
);

CREATE INDEX IF NOT EXISTS idx_sensitive_words_enabled
    ON sensitive_words (enabled);

CREATE TABLE IF NOT EXISTS admin_announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    scope VARCHAR(32) NOT NULL DEFAULT 'all',
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    read_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_admin_announcements_scope CHECK (scope IN ('all', 'active', 'risk', 'group')),
    CONSTRAINT ck_admin_announcements_status CHECK (status IN ('draft', 'published', 'withdrawn'))
);

CREATE INDEX IF NOT EXISTS idx_admin_announcements_status
    ON admin_announcements (status);

CREATE TABLE IF NOT EXISTS admin_dashboard_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL DEFAULT 'system',
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    level VARCHAR(16) NOT NULL DEFAULT 'info',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_admin_dashboard_events_level CHECK (level IN ('info', 'medium', 'high'))
);

CREATE INDEX IF NOT EXISTS idx_admin_dashboard_events_created_at
    ON admin_dashboard_events (created_at DESC);

CREATE TABLE IF NOT EXISTS admin_todo_items (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    "value" BIGINT NOT NULL DEFAULT 0,
    level VARCHAR(16) NOT NULL DEFAULT 'info',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_admin_todo_items_level CHECK (level IN ('info', 'medium', 'high'))
);

CREATE TABLE IF NOT EXISTS admin_metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    bucket_time TIMESTAMP NOT NULL UNIQUE,
    online_users BIGINT NOT NULL DEFAULT 0,
    messages BIGINT NOT NULL DEFAULT 0,
    sensitive_hits BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_metric_snapshots_bucket_time
    ON admin_metric_snapshots (bucket_time DESC);

INSERT INTO admin_todo_items (source_key, label, "value", level)
SELECT 'moment_reports', '待处理朋友圈举报', 0, 'high'
WHERE NOT EXISTS (SELECT 1 FROM admin_todo_items WHERE source_key = 'moment_reports');

INSERT INTO admin_todo_items (source_key, label, "value", level)
SELECT 'draft_announcements', '待发布公告', 0, 'medium'
WHERE NOT EXISTS (SELECT 1 FROM admin_todo_items WHERE source_key = 'draft_announcements');

INSERT INTO admin_todo_items (source_key, label, "value", level)
SELECT 'disabled_groups', '已禁用群聊', 0, 'medium'
WHERE NOT EXISTS (SELECT 1 FROM admin_todo_items WHERE source_key = 'disabled_groups');

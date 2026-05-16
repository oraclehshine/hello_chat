from app.db import execute_one, fetch_all, fetch_one


def dashboard_counts() -> dict | None:
    row = fetch_one(
        """
        SELECT
          (SELECT COUNT(*) FROM user_presence WHERE status = 'online') AS online_users,
          (SELECT COUNT(*) FROM users WHERE created_at::date = CURRENT_DATE) AS today_new_users,
          (SELECT COUNT(*) FROM user_presence WHERE last_active_at::date = CURRENT_DATE) AS today_active_users,
          (SELECT COUNT(*) FROM private_messages WHERE sent_at::date = CURRENT_DATE AND deleted_at IS NULL) AS private_messages,
          (SELECT COUNT(*) FROM group_messages WHERE sent_at::date = CURRENT_DATE AND deleted_at IS NULL) AS group_messages,
          (SELECT COUNT(*) FROM moments WHERE created_at::date = CURRENT_DATE AND deleted_at IS NULL) AS moments,
          (SELECT COUNT(*) FROM file_assets WHERE created_at::date = CURRENT_DATE) AS file_uploads
        """
    )
    return row


def refresh_dashboard_todos() -> bool:
    moment_reports = execute_one(
        """
        UPDATE admin_todo_items
        SET value = (SELECT COUNT(*) FROM moment_reports), updated_at = NOW()
        WHERE source_key = 'moment_reports'
        """
    )
    draft_announcements = execute_one(
        """
        UPDATE admin_todo_items
        SET value = (SELECT COUNT(*) FROM admin_announcements WHERE status = 'draft'), updated_at = NOW()
        WHERE source_key = 'draft_announcements'
        """
    )
    disabled_groups = execute_one(
        """
        UPDATE admin_todo_items
        SET value = (SELECT COUNT(*) FROM groups WHERE status <> 1), updated_at = NOW()
        WHERE source_key = 'disabled_groups'
        """
    )
    return all(item is not None for item in [moment_reports, draft_announcements, disabled_groups])


def dashboard_todo_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT label, value, level
        FROM admin_todo_items
        WHERE enabled = TRUE
        ORDER BY
          CASE level WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END,
          value DESC,
          id ASC
        LIMIT 10
        """
    )


def dashboard_event_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          event_type AS type,
          title,
          content,
          to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') AS time
        FROM admin_dashboard_events
        ORDER BY created_at DESC
        LIMIT 20
        """
    )


def refresh_metric_snapshot() -> dict | None:
    return execute_one(
        """
        INSERT INTO admin_metric_snapshots (bucket_time, online_users, messages, sensitive_hits)
        SELECT
          date_trunc('hour', NOW()) AS bucket_time,
          (SELECT COUNT(*) FROM user_presence WHERE status = 'online') AS online_users,
          (
            SELECT COUNT(*) FROM private_messages
            WHERE sent_at >= date_trunc('hour', NOW())
              AND sent_at < date_trunc('hour', NOW()) + INTERVAL '1 hour'
              AND deleted_at IS NULL
          ) + (
            SELECT COUNT(*) FROM group_messages
            WHERE sent_at >= date_trunc('hour', NOW())
              AND sent_at < date_trunc('hour', NOW()) + INTERVAL '1 hour'
              AND deleted_at IS NULL
          ) AS messages,
          (SELECT COALESCE(SUM(hit_count), 0) FROM sensitive_words) AS sensitive_hits
        ON CONFLICT (bucket_time) DO UPDATE
        SET
          online_users = EXCLUDED.online_users,
          messages = EXCLUDED.messages,
          sensitive_hits = EXCLUDED.sensitive_hits
        RETURNING id
        """
    )


def dashboard_trend_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          to_char(bucket_time, 'HH24:MI') AS label,
          online_users AS "onlineUsers",
          messages,
          sensitive_hits AS "sensitiveHits"
        FROM admin_metric_snapshots
        WHERE bucket_time >= date_trunc('hour', NOW()) - INTERVAL '11 hours'
        ORDER BY bucket_time ASC
        """
    )


def create_dashboard_event(
    event_type: str,
    title: str,
    content: str,
    level: str = "info",
) -> bool:
    row = execute_one(
        """
        INSERT INTO admin_dashboard_events (event_type, title, content, level)
        VALUES (:event_type, :title, :content, :level)
        RETURNING id
        """,
        {
            "event_type": event_type,
            "title": title,
            "content": content,
            "level": level,
        },
    )
    return row is not None


def user_rows(keyword: str = "") -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          u.id AS "userId",
          u.email,
          u.nickname,
          lower(u.status) AS status,
          COALESCE(up.status = 'online', false) AS online,
          (
            SELECT COUNT(*) FROM private_messages pm
            WHERE pm.sender_id = u.id AND pm.deleted_at IS NULL
          ) + (
            SELECT COUNT(*) FROM group_messages gm
            WHERE gm.sender_id = u.id AND gm.deleted_at IS NULL
          ) AS "messageCount",
          (
            SELECT COUNT(*) FROM group_members gmem
            WHERE gmem.user_id = u.id AND gmem.left_at IS NULL
          ) AS "groupCount",
          CASE
            WHEN u.status <> 'ACTIVE' THEN 'high'
            WHEN (
              SELECT COUNT(*) FROM moment_reports mr
              LEFT JOIN moments m ON m.id = mr.moment_id
              WHERE mr.reporter_id = u.id OR m.author_id = u.id
            ) > 0 THEN 'medium'
            ELSE 'low'
          END AS "riskLevel",
          COALESCE(to_char(up.last_active_at, 'YYYY-MM-DD HH24:MI:SS'), to_char(u.updated_at, 'YYYY-MM-DD HH24:MI:SS')) AS "lastActiveAt"
        FROM users u
        LEFT JOIN user_presence up ON up.user_id = u.id
        WHERE (:keyword = '' OR lower(u.email) LIKE :like_keyword OR lower(u.nickname) LIKE :like_keyword)
        ORDER BY u.id DESC
        LIMIT 100
        """,
        {"keyword": keyword.lower(), "like_keyword": f"%{keyword.lower()}%"},
    )


def group_rows(keyword: str = "") -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          g.id AS "groupId",
          g.name AS "groupName",
          u.nickname AS "ownerName",
          CASE WHEN g.status = 1 THEN 'active' ELSE 'disabled' END AS status,
          (
            SELECT COUNT(*) FROM group_members gm
            WHERE gm.group_id = g.id AND gm.left_at IS NULL
          ) AS "memberCount",
          (
            SELECT COUNT(*) FROM group_messages msg
            WHERE msg.group_id = g.id AND msg.sent_at::date = CURRENT_DATE AND msg.deleted_at IS NULL
          ) AS "todayMessages",
          0 AS "sensitiveHits",
          0 AS "reportCount"
        FROM groups g
        JOIN users u ON u.id = g.owner_id
        WHERE (:keyword = '' OR lower(g.name) LIKE :like_keyword)
        ORDER BY g.id DESC
        LIMIT 100
        """,
        {"keyword": keyword.lower(), "like_keyword": f"%{keyword.lower()}%"},
    )


def audit_log_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          id AS "logId",
          admin_name AS "adminName",
          module,
          action,
          target,
          ip,
          to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') AS "createdAt"
        FROM admin_operation_logs
        ORDER BY created_at DESC
        LIMIT 100
        """
    )


def sensitive_word_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          id AS "wordId",
          word,
          scene,
          level,
          action,
          enabled,
          hit_count AS "hitCount"
        FROM sensitive_words
        ORDER BY id DESC
        LIMIT 100
        """
    )


def announcement_rows() -> list[dict] | None:
    return fetch_all(
        """
        SELECT
          id AS "announcementId",
          title,
          scope,
          status,
          read_count AS "readCount",
          to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') AS "createdAt"
        FROM admin_announcements
        ORDER BY created_at DESC
        LIMIT 100
        """
    )


def create_audit_log(
    module: str,
    action: str,
    target: str,
    admin_name: str = "平台管理员",
    ip: str = "127.0.0.1",
) -> bool:
    row = execute_one(
        """
        INSERT INTO admin_operation_logs (admin_name, module, action, target, ip)
        VALUES (:admin_name, :module, :action, :target, :ip)
        RETURNING id
        """,
        {
            "admin_name": admin_name,
            "module": module,
            "action": action,
            "target": target,
            "ip": ip,
        },
    )
    if row is None:
        return False
    create_dashboard_event("audit", f"{module}操作", f"{action}：{target}")
    return True


def set_user_status(user_id: int, status: str) -> dict | None:
    return execute_one(
        """
        UPDATE users
        SET status = :status, updated_at = NOW()
        WHERE id = :user_id
        RETURNING id AS "userId", lower(status) AS status
        """,
        {"user_id": user_id, "status": status},
    )


def set_group_status(group_id: int, status: int) -> dict | None:
    return execute_one(
        """
        UPDATE groups
        SET status = :status, updated_at = NOW()
        WHERE id = :group_id
        RETURNING id AS "groupId", CASE WHEN status = 1 THEN 'active' ELSE 'disabled' END AS status
        """,
        {"group_id": group_id, "status": status},
    )


def create_sensitive_word(payload: dict) -> dict | None:
    return execute_one(
        """
        INSERT INTO sensitive_words (word, scene, level, action, enabled)
        VALUES (:word, :scene, :level, :action, :enabled)
        RETURNING
          id AS "wordId",
          word,
          scene,
          level,
          action,
          enabled,
          hit_count AS "hitCount"
        """,
        payload,
    )


def set_sensitive_word_enabled(word_id: int, enabled: bool) -> dict | None:
    return execute_one(
        """
        UPDATE sensitive_words
        SET enabled = :enabled, updated_at = NOW()
        WHERE id = :word_id
        RETURNING
          id AS "wordId",
          word,
          scene,
          level,
          action,
          enabled,
          hit_count AS "hitCount"
        """,
        {"word_id": word_id, "enabled": enabled},
    )


def delete_sensitive_word(word_id: int) -> bool | None:
    row = execute_one(
        "DELETE FROM sensitive_words WHERE id = :word_id",
        {"word_id": word_id},
    )
    if row is None:
        return None
    return int(row.get("affected") or 0) > 0


def create_announcement(payload: dict) -> dict | None:
    return execute_one(
        """
        INSERT INTO admin_announcements (title, content, scope, status)
        VALUES (:title, :content, :scope, :status)
        RETURNING
          id AS "announcementId",
          title,
          scope,
          status,
          read_count AS "readCount",
          to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') AS "createdAt"
        """,
        payload,
    )


def set_announcement_status(announcement_id: int, status: str) -> dict | None:
    return execute_one(
        """
        UPDATE admin_announcements
        SET status = :status, updated_at = NOW()
        WHERE id = :announcement_id
        RETURNING
          id AS "announcementId",
          title,
          scope,
          status,
          read_count AS "readCount",
          to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') AS "createdAt"
        """,
        {"announcement_id": announcement_id, "status": status},
    )


def delete_announcement(announcement_id: int) -> bool | None:
    row = execute_one(
        "DELETE FROM admin_announcements WHERE id = :announcement_id",
        {"announcement_id": announcement_id},
    )
    if row is None:
        return None
    return int(row.get("affected") or 0) > 0

package com.hellochat.backend.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);
    private static final int EVENT_RETENTION = 500;

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(
        initialDelayString = "${hello-chat.admin-dashboard.initial-delay-ms:15000}",
        fixedDelayString = "${hello-chat.admin-dashboard.refresh-ms:60000}"
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduledRefresh() {
        runSafely(() -> {
            refreshTodoItems();
            refreshMetricSnapshot();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPrivateMessage(Long chatId, Long senderId, String messageType, String content) {
        runSafely(() -> {
            insertEvent("chat", "私聊消息", "用户 " + senderId + " 在会话 " + chatId + " 发送 " + messageType + " 消息", "info");
            detectSensitiveWords("chat", content, "私聊敏感词命中", "会话 " + chatId + "，发送者 " + senderId);
            refreshMetricSnapshot();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGroupMessage(Long groupId, Long senderId, String messageType, String content) {
        runSafely(() -> {
            insertEvent("group", "群聊消息", "用户 " + senderId + " 在群聊 " + groupId + " 发送 " + messageType + " 消息", "info");
            detectSensitiveWords("group", content, "群聊敏感词命中", "群聊 " + groupId + "，发送者 " + senderId);
            refreshMetricSnapshot();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGroupCreated(Long groupId, Long ownerId, String groupName) {
        runSafely(() -> {
            insertEvent("group", "新建群聊", "用户 " + ownerId + " 创建群聊 " + safeText(groupName, 30) + "（ID " + groupId + "）", "info");
            refreshTodoItems();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMomentCreated(Long momentId, Long authorId, String content) {
        runSafely(() -> {
            insertEvent("moment", "朋友圈动态", "用户 " + authorId + " 发布朋友圈动态 " + momentId, "info");
            detectSensitiveWords("moment", content, "朋友圈敏感词命中", "动态 " + momentId + "，作者 " + authorId);
            refreshMetricSnapshot();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMomentReported(Long momentId, Long reporterId) {
        runSafely(() -> {
            insertEvent("report", "新的朋友圈举报", "用户 " + reporterId + " 举报动态 " + momentId, "high");
            refreshTodoItems();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMomentReportReviewed(Long reportId, Long reviewerId, Integer status) {
        runSafely(() -> {
            insertEvent("report", "朋友圈举报已处理", "管理员 " + reviewerId + " 处理举报 " + reportId + "，状态 " + status, "info");
            refreshTodoItems();
            pruneEvents();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPresence(Long userId, String status) {
        runSafely(() -> {
            if ("online".equals(status) || "offline".equals(status)) {
                insertEvent("presence", "用户状态变化", "用户 " + userId + " 状态变为 " + status, "info");
                pruneEvents();
            }
            refreshMetricSnapshot();
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFileUpload(Long uploaderId, Long fileId, String scene, long fileSize) {
        runSafely(() -> {
            insertEvent("file", "文件上传", "用户 " + uploaderId + " 上传文件 " + fileId + "，场景 " + scene + "，大小 " + fileSize + " 字节", "info");
            refreshMetricSnapshot();
            pruneEvents();
        });
    }

    private void detectSensitiveWords(String scene, String content, String title, String context) {
        if (content == null || content.isBlank()) {
            return;
        }
        List<Map<String, Object>> words = jdbcTemplate.queryForList(
            "SELECT id, word, level FROM sensitive_words WHERE enabled = TRUE AND scene IN (?, 'all')",
            scene
        );
        String lowerContent = content.toLowerCase();
        for (Map<String, Object> row : words) {
            String word = String.valueOf(row.get("word"));
            if (!word.isBlank() && lowerContent.contains(word.toLowerCase())) {
                Number id = (Number) row.get("id");
                String level = String.valueOf(row.get("level"));
                jdbcTemplate.update("UPDATE sensitive_words SET hit_count = hit_count + 1, updated_at = NOW() WHERE id = ?", id.longValue());
                insertEvent("sensitive", title, context + "，词条 " + word, "high".equals(level) ? "high" : "medium");
            }
        }
    }

    private void refreshTodoItems() {
        jdbcTemplate.update(
            """
            UPDATE admin_todo_items
            SET "value" = (SELECT COUNT(*) FROM moment_reports WHERE status = 1), updated_at = NOW()
            WHERE source_key = 'moment_reports'
            """
        );
        jdbcTemplate.update(
            """
            UPDATE admin_todo_items
            SET "value" = (SELECT COUNT(*) FROM admin_announcements WHERE status = 'draft'), updated_at = NOW()
            WHERE source_key = 'draft_announcements'
            """
        );
        jdbcTemplate.update(
            """
            UPDATE admin_todo_items
            SET "value" = (SELECT COUNT(*) FROM groups WHERE status <> 1), updated_at = NOW()
            WHERE source_key = 'disabled_groups'
            """
        );
    }

    private void refreshMetricSnapshot() {
        jdbcTemplate.update(
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
            """
        );
    }

    private void insertEvent(String eventType, String title, String content, String level) {
        jdbcTemplate.update(
            """
            INSERT INTO admin_dashboard_events (event_type, title, content, level)
            VALUES (?, ?, ?, ?)
            """,
            eventType,
            title,
            content,
            level
        );
    }

    private void pruneEvents() {
        jdbcTemplate.update(
            """
            DELETE FROM admin_dashboard_events
            WHERE id NOT IN (
              SELECT id FROM admin_dashboard_events
              ORDER BY created_at DESC
              LIMIT ?
            )
            """,
            EVENT_RETENTION
        );
    }

    private String safeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "未命名";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private void runSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ex) {
            log.warn("Admin dashboard integration skipped: {}", ex.getMessage());
        }
    }
}

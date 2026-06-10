package com.hellochat.backend.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupHotCacheService {

    private static final String SUMMARY_KEY_PREFIX = "hello-chat:group:summary:user:";
    private static final String UNREAD_KEY_PREFIX = "hello-chat:group:unread:user:";
    private static final String MENTION_UNREAD_KEY_PREFIX = "hello-chat:group:mention-unread:user:";
    private static final String NOTICE_UNREAD_KEY_PREFIX = "hello-chat:group:notice-unread:user:";
    private static final Duration SUMMARY_TTL = Duration.ofHours(6);
    private static final Duration COUNTER_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public GroupHotCacheService(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheSummary(
        Long userId,
        Long groupId,
        Long lastMessageId,
        String lastMessageType,
        String lastMessagePreview,
        LocalDateTime lastMessageAt
    ) {
        GroupSummaryCacheValue value = new GroupSummaryCacheValue();
        value.setLastMessageId(lastMessageId);
        value.setLastMessageType(lastMessageType);
        value.setLastMessagePreview(lastMessagePreview);
        value.setLastMessageAt(lastMessageAt);
        try {
            stringRedisTemplate.opsForValue().set(summaryKey(userId, groupId), objectMapper.writeValueAsString(value), SUMMARY_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize group summary cache", ex);
        }
    }

    public Optional<GroupSummaryCacheValue> getSummary(Long userId, Long groupId) {
        String value = stringRedisTemplate.opsForValue().get(summaryKey(userId, groupId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, GroupSummaryCacheValue.class));
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.delete(summaryKey(userId, groupId));
            return Optional.empty();
        }
    }

    public long getUnreadCount(Long userId, Long groupId) {
        return getLong(unreadKey(userId, groupId));
    }

    public void setUnreadCount(Long userId, Long groupId, long unreadCount) {
        setLong(unreadKey(userId, groupId), unreadCount);
    }

    public long incrementUnreadCount(Long userId, Long groupId) {
        return increment(unreadKey(userId, groupId));
    }

    public void resetUnreadCount(Long userId, Long groupId) {
        setUnreadCount(userId, groupId, 0L);
    }

    public long getMentionUnreadCount(Long userId, Long groupId) {
        return getLong(mentionUnreadKey(userId, groupId));
    }

    public void setMentionUnreadCount(Long userId, Long groupId, long unreadCount) {
        setLong(mentionUnreadKey(userId, groupId), unreadCount);
    }

    public long incrementMentionUnreadCount(Long userId, Long groupId) {
        return increment(mentionUnreadKey(userId, groupId));
    }

    public void resetMentionUnreadCount(Long userId, Long groupId) {
        setMentionUnreadCount(userId, groupId, 0L);
    }

    public Boolean getNoticeUnread(Long userId, Long groupId) {
        String value = stringRedisTemplate.opsForValue().get(noticeUnreadKey(userId, groupId));
        if (value == null || value.isBlank()) {
            return null;
        }
        return "1".equals(value);
    }

    public void setNoticeUnread(Long userId, Long groupId, boolean unread) {
        stringRedisTemplate.opsForValue().set(noticeUnreadKey(userId, groupId), unread ? "1" : "0", COUNTER_TTL);
    }

    private long getLong(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            stringRedisTemplate.delete(key);
            return -1L;
        }
    }

    private void setLong(String key, long value) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(Math.max(value, 0L)), COUNTER_TTL);
    }

    private long increment(String key) {
        Long value = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, COUNTER_TTL);
        return value == null ? 0L : value;
    }

    private String summaryKey(Long userId, Long groupId) {
        return SUMMARY_KEY_PREFIX + userId + ":group:" + groupId;
    }

    private String unreadKey(Long userId, Long groupId) {
        return UNREAD_KEY_PREFIX + userId + ":group:" + groupId;
    }

    private String mentionUnreadKey(Long userId, Long groupId) {
        return MENTION_UNREAD_KEY_PREFIX + userId + ":group:" + groupId;
    }

    private String noticeUnreadKey(Long userId, Long groupId) {
        return NOTICE_UNREAD_KEY_PREFIX + userId + ":group:" + groupId;
    }
}

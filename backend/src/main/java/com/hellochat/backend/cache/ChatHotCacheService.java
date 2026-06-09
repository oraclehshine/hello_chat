package com.hellochat.backend.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatHotCacheService {

    private static final String SUMMARY_KEY_PREFIX = "hello-chat:chat:summary:user:";
    private static final String UNREAD_KEY_PREFIX = "hello-chat:chat:unread:user:";
    private static final Duration SUMMARY_TTL = Duration.ofHours(6);
    private static final Duration UNREAD_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ChatHotCacheService(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheSummary(
        Long userId,
        Long chatId,
        Long lastMessageId,
        String lastMessageType,
        String lastMessagePreview,
        LocalDateTime lastMessageAt
    ) {
        ChatSummaryCacheValue value = new ChatSummaryCacheValue();
        value.setLastMessageId(lastMessageId);
        value.setLastMessageType(lastMessageType);
        value.setLastMessagePreview(lastMessagePreview);
        value.setLastMessageAt(lastMessageAt);
        try {
            stringRedisTemplate.opsForValue().set(summaryKey(userId, chatId), objectMapper.writeValueAsString(value), SUMMARY_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize chat summary cache", ex);
        }
    }

    public Optional<ChatSummaryCacheValue> getSummary(Long userId, Long chatId) {
        String value = stringRedisTemplate.opsForValue().get(summaryKey(userId, chatId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, ChatSummaryCacheValue.class));
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.delete(summaryKey(userId, chatId));
            return Optional.empty();
        }
    }

    public long getUnreadCount(Long userId, Long chatId) {
        String value = stringRedisTemplate.opsForValue().get(unreadKey(userId, chatId));
        if (value == null || value.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            stringRedisTemplate.delete(unreadKey(userId, chatId));
            return -1L;
        }
    }

    public void setUnreadCount(Long userId, Long chatId, long unreadCount) {
        stringRedisTemplate.opsForValue().set(unreadKey(userId, chatId), String.valueOf(Math.max(unreadCount, 0L)), UNREAD_TTL);
    }

    public long incrementUnreadCount(Long userId, Long chatId) {
        Long value = stringRedisTemplate.opsForValue().increment(unreadKey(userId, chatId));
        stringRedisTemplate.expire(unreadKey(userId, chatId), UNREAD_TTL);
        return value == null ? 0L : value;
    }

    public void resetUnreadCount(Long userId, Long chatId) {
        setUnreadCount(userId, chatId, 0L);
    }

    private String summaryKey(Long userId, Long chatId) {
        return SUMMARY_KEY_PREFIX + userId + ":chat:" + chatId;
    }

    private String unreadKey(Long userId, Long chatId) {
        return UNREAD_KEY_PREFIX + userId + ":chat:" + chatId;
    }
}

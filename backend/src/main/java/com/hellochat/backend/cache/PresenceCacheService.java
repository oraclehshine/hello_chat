package com.hellochat.backend.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellochat.backend.entity.UserPresence;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceCacheService {

    private static final String KEY_PREFIX = "hello-chat:presence:user:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PresenceCacheService(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void put(Long userId, String status, LocalDateTime lastActiveAt, LocalDateTime updatedAt) {
        UserPresence presence = new UserPresence();
        presence.setUserId(userId);
        presence.setStatus(status);
        presence.setLastActiveAt(lastActiveAt == null ? LocalDateTime.now() : lastActiveAt);
        presence.setUpdatedAt(updatedAt == null ? LocalDateTime.now() : updatedAt);
        try {
            stringRedisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(presence), TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize presence cache", ex);
        }
    }

    public Optional<UserPresence> get(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(key(userId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, UserPresence.class));
        } catch (JsonProcessingException ex) {
            stringRedisTemplate.delete(key(userId));
            return Optional.empty();
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}

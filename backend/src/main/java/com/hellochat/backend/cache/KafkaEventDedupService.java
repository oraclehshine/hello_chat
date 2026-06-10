package com.hellochat.backend.cache;

import com.hellochat.backend.config.HighConcurrencyProperties;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventDedupService {

    private static final String KEY_PREFIX = "hello-chat:kafka:event:";

    private final StringRedisTemplate stringRedisTemplate;
    private final HighConcurrencyProperties properties;

    public KafkaEventDedupService(
        StringRedisTemplate stringRedisTemplate,
        HighConcurrencyProperties properties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public boolean shouldProcess(String channel, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        Boolean accepted = stringRedisTemplate.opsForValue().setIfAbsent(
            key(channel, eventId),
            "1",
            Duration.ofSeconds(Math.max(properties.getKafkaEventDedupTtlSeconds(), 60L))
        );
        return Boolean.TRUE.equals(accepted);
    }

    private String key(String channel, String eventId) {
        return KEY_PREFIX + channel + ":" + eventId;
    }
}

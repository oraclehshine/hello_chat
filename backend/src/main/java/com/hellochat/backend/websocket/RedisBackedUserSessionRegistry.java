package com.hellochat.backend.websocket;

import com.hellochat.backend.cache.PresenceCacheService;
import com.hellochat.backend.config.HighConcurrencyProperties;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RedisBackedUserSessionRegistry implements UserSessionRegistry {

    private static final String ROUTE_KEY_PREFIX = "hello-chat:ws:user:";

    private final Map<Long, Map<String, WebSocketSession>> localSessions = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;
    private final HighConcurrencyProperties properties;
    private final PresenceCacheService presenceCacheService;

    public RedisBackedUserSessionRegistry(
        StringRedisTemplate stringRedisTemplate,
        HighConcurrencyProperties properties,
        PresenceCacheService presenceCacheService
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.presenceCacheService = presenceCacheService;
    }

    @Override
    public void registerSession(Long userId, WebSocketSession session) {
        localSessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
            .put(session.getId(), session);
        touchRoute(userId);
        LocalDateTime now = LocalDateTime.now();
        presenceCacheService.put(userId, "online", now, now);
    }

    @Override
    public void unregisterSession(Long userId, WebSocketSession session) {
        Map<String, WebSocketSession> userSessions = localSessions.get(userId);
        if (userSessions == null) {
            removeRouteNode(userId);
            markOfflineIfNoRoute(userId);
            return;
        }
        userSessions.remove(session.getId());
        if (userSessions.isEmpty()) {
            localSessions.remove(userId);
            removeRouteNode(userId);
            markOfflineIfNoRoute(userId);
            return;
        }
        touchRoute(userId);
    }

    @Override
    public int pushToUser(Long userId, String payload) {
        Map<String, WebSocketSession> userSessions = localSessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return 0;
        }
        int successCount = 0;
        Set<String> closedSessionIds = new LinkedHashSet<>();
        TextMessage message = new TextMessage(payload);
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session == null || !session.isOpen()) {
                closedSessionIds.add(entry.getKey());
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
                successCount++;
            } catch (IOException ex) {
                closedSessionIds.add(entry.getKey());
            }
        }
        for (String sessionId : closedSessionIds) {
            userSessions.remove(sessionId);
        }
        if (userSessions.isEmpty()) {
            localSessions.remove(userId);
            removeRouteNode(userId);
            markOfflineIfNoRoute(userId);
        } else {
            touchRoute(userId);
            LocalDateTime now = LocalDateTime.now();
            presenceCacheService.put(userId, "online", now, now);
        }
        return successCount;
    }

    @Override
    public Set<String> getRouteNodes(Long userId) {
        Set<String> routeNodes = stringRedisTemplate.opsForSet().members(routeKey(userId));
        return routeNodes == null ? Collections.emptySet() : routeNodes;
    }

    private void touchRoute(Long userId) {
        String key = routeKey(userId);
        stringRedisTemplate.opsForSet().add(key, properties.getNodeId());
        stringRedisTemplate.expire(key, Duration.ofSeconds(properties.getWebsocketRouteTtlSeconds()));
    }

    private void removeRouteNode(Long userId) {
        String key = routeKey(userId);
        stringRedisTemplate.opsForSet().remove(key, properties.getNodeId());
    }

    private String routeKey(Long userId) {
        return ROUTE_KEY_PREFIX + userId + ":nodes";
    }

    private void markOfflineIfNoRoute(Long userId) {
        if (getRouteNodes(userId).isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            presenceCacheService.put(userId, "offline", now, now);
        }
    }
}

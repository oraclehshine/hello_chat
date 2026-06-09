package com.hellochat.backend.websocket;

import java.util.Set;
import org.springframework.web.socket.WebSocketSession;

public interface UserSessionRegistry {

    void registerSession(Long userId, WebSocketSession session);

    void unregisterSession(Long userId, WebSocketSession session);

    int pushToUser(Long userId, String payload);

    Set<String> getRouteNodes(Long userId);
}

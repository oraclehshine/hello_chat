package com.hellochat.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final UserSessionRegistry userSessionRegistry;

    public ChatWebSocketHandler(UserSessionRegistry userSessionRegistry) {
        this.userSessionRegistry = userSessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get(TokenHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (!(userId instanceof Long value)) {
            throw new IllegalArgumentException("userId is required");
        }
        userSessionRegistry.registerSession(value, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(TokenHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (userId instanceof Long value) {
            userSessionRegistry.unregisterSession(value, session);
        }
    }

    public void push(Long userId, String payload) {
        userSessionRegistry.pushToUser(userId, payload);
    }
}

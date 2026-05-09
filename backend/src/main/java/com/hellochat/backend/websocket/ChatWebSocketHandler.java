package com.hellochat.backend.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get(TokenHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (!(userId instanceof Long value)) {
            throw new IllegalArgumentException("userId is required");
        }
        sessions.put(value, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(TokenHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (userId instanceof Long value) {
            sessions.remove(value);
        }
    }

    public void push(Long userId, String payload) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            sessions.remove(userId);
            return;
        }
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ex) {
            sessions.remove(userId);
        }
    }
}

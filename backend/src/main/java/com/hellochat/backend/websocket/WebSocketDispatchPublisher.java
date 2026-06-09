package com.hellochat.backend.websocket;

public interface WebSocketDispatchPublisher {

    void publish(Long userId, String eventType, String payload);
}

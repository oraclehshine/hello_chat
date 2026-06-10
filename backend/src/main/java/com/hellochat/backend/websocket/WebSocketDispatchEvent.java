package com.hellochat.backend.websocket;

public class WebSocketDispatchEvent {

    private String eventId;
    private Long userId;
    private String eventType;
    private String payload;
    private String sourceNodeId;

    public WebSocketDispatchEvent() {
    }

    public WebSocketDispatchEvent(String eventId, Long userId, String eventType, String payload, String sourceNodeId) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.payload = payload;
        this.sourceNodeId = sourceNodeId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }
}

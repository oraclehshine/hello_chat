package com.hellochat.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hello-chat.concurrency")
public class HighConcurrencyProperties {

    private String nodeId = "hello-chat-node";
    private long websocketRouteTtlSeconds = 120;
    private boolean websocketKafkaEnabled = false;
    private String websocketDispatchTopic = "hello-chat-websocket-dispatch";
    private String chatPushTopic = "hello-chat-chat-push";
    private String groupPushTopic = "hello-chat-group-push";
    private long kafkaEventDedupTtlSeconds = 600L;
    private int eventCorePoolSize = 4;
    private int eventMaxPoolSize = 16;
    private int eventQueueCapacity = 1000;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getWebsocketRouteTtlSeconds() {
        return websocketRouteTtlSeconds;
    }

    public void setWebsocketRouteTtlSeconds(long websocketRouteTtlSeconds) {
        this.websocketRouteTtlSeconds = websocketRouteTtlSeconds;
    }

    public boolean isWebsocketKafkaEnabled() {
        return websocketKafkaEnabled;
    }

    public void setWebsocketKafkaEnabled(boolean websocketKafkaEnabled) {
        this.websocketKafkaEnabled = websocketKafkaEnabled;
    }

    public String getWebsocketDispatchTopic() {
        return websocketDispatchTopic;
    }

    public void setWebsocketDispatchTopic(String websocketDispatchTopic) {
        this.websocketDispatchTopic = websocketDispatchTopic;
    }

    public String getChatPushTopic() {
        return chatPushTopic;
    }

    public void setChatPushTopic(String chatPushTopic) {
        this.chatPushTopic = chatPushTopic;
    }

    public String getGroupPushTopic() {
        return groupPushTopic;
    }

    public void setGroupPushTopic(String groupPushTopic) {
        this.groupPushTopic = groupPushTopic;
    }

    public long getKafkaEventDedupTtlSeconds() {
        return kafkaEventDedupTtlSeconds;
    }

    public void setKafkaEventDedupTtlSeconds(long kafkaEventDedupTtlSeconds) {
        this.kafkaEventDedupTtlSeconds = kafkaEventDedupTtlSeconds;
    }

    public int getEventCorePoolSize() {
        return eventCorePoolSize;
    }

    public void setEventCorePoolSize(int eventCorePoolSize) {
        this.eventCorePoolSize = eventCorePoolSize;
    }

    public int getEventMaxPoolSize() {
        return eventMaxPoolSize;
    }

    public void setEventMaxPoolSize(int eventMaxPoolSize) {
        this.eventMaxPoolSize = eventMaxPoolSize;
    }

    public int getEventQueueCapacity() {
        return eventQueueCapacity;
    }

    public void setEventQueueCapacity(int eventQueueCapacity) {
        this.eventQueueCapacity = eventQueueCapacity;
    }
}

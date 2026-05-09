package com.hellochat.backend.dto;

import com.hellochat.backend.entity.Notification;
import java.time.LocalDateTime;

public class NotificationResponse {

    private final Long notificationId;
    private final String notificationType;
    private final String title;
    private final String content;
    private final Long relatedId;
    private final Integer read;
    private final LocalDateTime createdAt;

    public NotificationResponse(Notification notification) {
        this.notificationId = notification.getId();
        this.notificationType = notification.getNotificationType();
        this.title = notification.getTitle();
        this.content = notification.getContent();
        this.relatedId = notification.getRelatedId();
        this.read = notification.getRead();
        this.createdAt = notification.getCreatedAt();
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public Integer getRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

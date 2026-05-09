package com.hellochat.backend.dto;

import com.hellochat.backend.entity.GroupNotification;
import java.time.LocalDateTime;

public class GroupNotificationResponse {

    private final Long notificationId;
    private final Long groupId;
    private final Long actorId;
    private final Long targetUserId;
    private final String noticeType;
    private final String content;
    private final LocalDateTime createdAt;

    public GroupNotificationResponse(GroupNotification notification) {
        this.notificationId = notification.getId();
        this.groupId = notification.getGroupId();
        this.actorId = notification.getActorId();
        this.targetUserId = notification.getTargetUserId();
        this.noticeType = notification.getNoticeType();
        this.content = notification.getContent();
        this.createdAt = notification.getCreatedAt();
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getActorId() {
        return actorId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

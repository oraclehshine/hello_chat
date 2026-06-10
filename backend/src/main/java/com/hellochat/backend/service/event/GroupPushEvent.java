package com.hellochat.backend.service.event;

import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupResponse;

public class GroupPushEvent {

    public static final String TYPE_NEW_MESSAGE = "new_message";
    public static final String TYPE_MESSAGE_UPDATED = "message_updated";
    public static final String TYPE_GROUP_UPDATED = "group_updated";

    private String eventId;
    private String type;
    private Long groupId;
    private Long actorUserId;
    private Long messageId;
    private String eventType;
    private GroupMessageResponse message;
    private GroupResponse group;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public GroupMessageResponse getMessage() {
        return message;
    }

    public void setMessage(GroupMessageResponse message) {
        this.message = message;
    }

    public GroupResponse getGroup() {
        return group;
    }

    public void setGroup(GroupResponse group) {
        this.group = group;
    }
}

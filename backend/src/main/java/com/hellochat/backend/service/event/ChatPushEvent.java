package com.hellochat.backend.service.event;

import com.hellochat.backend.dto.PrivateMessageResponse;

public class ChatPushEvent {

    public static final String TYPE_NEW_MESSAGE = "new_message";
    public static final String TYPE_MESSAGE_UPDATED = "message_updated";
    public static final String TYPE_READ_RECEIPT = "read_receipt";
    public static final String TYPE_TYPING_STATUS = "typing_status";

    private String type;
    private Long chatId;
    private Long actorUserId;
    private Long messageId;
    private Long lastReadMessageId;
    private String eventType;
    private Boolean typing;
    private PrivateMessageResponse message;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
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

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Boolean getTyping() {
        return typing;
    }

    public void setTyping(Boolean typing) {
        this.typing = typing;
    }

    public PrivateMessageResponse getMessage() {
        return message;
    }

    public void setMessage(PrivateMessageResponse message) {
        this.message = message;
    }
}

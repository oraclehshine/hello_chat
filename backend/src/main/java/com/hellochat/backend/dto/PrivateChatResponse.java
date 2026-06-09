package com.hellochat.backend.dto;

import com.hellochat.backend.cache.ChatSummaryCacheValue;
import com.hellochat.backend.entity.PrivateChat;
import com.hellochat.backend.entity.PrivateMessage;
import java.time.LocalDateTime;

public class PrivateChatResponse {

    private final Long chatId;
    private final Long targetUserId;
    private final String targetEmail;
    private final String targetNickname;
    private final String targetAvatarUrl;
    private final Long lastMessageId;
    private final String lastMessageType;
    private final String lastMessagePreview;
    private final LocalDateTime lastMessageAt;
    private final long unreadCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PrivateChatResponse(PrivateChat chat, UserProfileResponse targetUser) {
        this(chat, targetUser, (PrivateMessage) null, 0);
    }

    public PrivateChatResponse(PrivateChat chat, UserProfileResponse targetUser, PrivateMessage lastMessage) {
        this(chat, targetUser, lastMessage, 0);
    }

    public PrivateChatResponse(PrivateChat chat, UserProfileResponse targetUser, PrivateMessage lastMessage, long unreadCount) {
        this.chatId = chat.getId();
        this.targetUserId = targetUser.getUserId();
        this.targetEmail = targetUser.getEmail();
        this.targetNickname = targetUser.getNickname();
        this.targetAvatarUrl = targetUser.getAvatarUrl();
        this.lastMessageId = chat.getLastMessageId();
        this.lastMessageType = lastMessage == null ? null : lastMessage.getMessageType();
        this.lastMessagePreview = lastMessage == null ? null : preview(lastMessage);
        this.lastMessageAt = chat.getLastMessageAt();
        this.unreadCount = unreadCount;
        this.createdAt = chat.getCreatedAt();
        this.updatedAt = chat.getUpdatedAt();
    }

    public PrivateChatResponse(
        PrivateChat chat,
        UserProfileResponse targetUser,
        ChatSummaryCacheValue cachedSummary,
        long unreadCount
    ) {
        this.chatId = chat.getId();
        this.targetUserId = targetUser.getUserId();
        this.targetEmail = targetUser.getEmail();
        this.targetNickname = targetUser.getNickname();
        this.targetAvatarUrl = targetUser.getAvatarUrl();
        this.lastMessageId = cachedSummary == null ? chat.getLastMessageId() : cachedSummary.getLastMessageId();
        this.lastMessageType = cachedSummary == null ? null : cachedSummary.getLastMessageType();
        this.lastMessagePreview = cachedSummary == null ? null : cachedSummary.getLastMessagePreview();
        this.lastMessageAt = cachedSummary == null ? chat.getLastMessageAt() : cachedSummary.getLastMessageAt();
        this.unreadCount = unreadCount;
        this.createdAt = chat.getCreatedAt();
        this.updatedAt = chat.getUpdatedAt();
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public String getTargetNickname() {
        return targetNickname;
    }

    public String getTargetAvatarUrl() {
        return targetAvatarUrl;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private String preview(PrivateMessage message) {
        if (message.getRecallStatus() != null && message.getRecallStatus() == PrivateMessage.RECALL_RECALLED) {
            return "Message recalled";
        }
        if ("image".equals(message.getMessageType())) {
            return "[Image]";
        }
        if ("file".equals(message.getMessageType())) {
            return "[File]";
        }
        String content = message.getContent() == null ? "" : message.getContent().trim();
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }
}

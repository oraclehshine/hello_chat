package com.hellochat.backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.GroupMessage;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;
import java.util.List;

public class GroupMessageResponse {

    private final Long messageId;
    private final Long groupId;
    private final Long senderId;
    private final String senderNickname;
    private final String senderAvatarUrl;
    private final String messageType;
    private final String content;
    private final Long fileId;
    private final Long replyToMessageId;
    private final String replyPreview;
    private final List<Long> mentionUserIds;
    private final String fileName;
    private final String fileMimeType;
    private final Long fileSize;
    private final Integer mentionAll;
    private final Integer recallStatus;
    private final LocalDateTime sentAt;
    private final LocalDateTime updatedAt;

    public GroupMessageResponse(GroupMessage message, User sender, FileAsset fileAsset) {
        this(message, sender, fileAsset, null, List.of());
    }

    @JsonCreator
    public GroupMessageResponse(
        @JsonProperty("messageId") Long messageId,
        @JsonProperty("groupId") Long groupId,
        @JsonProperty("senderId") Long senderId,
        @JsonProperty("senderNickname") String senderNickname,
        @JsonProperty("senderAvatarUrl") String senderAvatarUrl,
        @JsonProperty("messageType") String messageType,
        @JsonProperty("content") String content,
        @JsonProperty("fileId") Long fileId,
        @JsonProperty("replyToMessageId") Long replyToMessageId,
        @JsonProperty("replyPreview") String replyPreview,
        @JsonProperty("mentionUserIds") List<Long> mentionUserIds,
        @JsonProperty("fileName") String fileName,
        @JsonProperty("fileMimeType") String fileMimeType,
        @JsonProperty("fileSize") Long fileSize,
        @JsonProperty("mentionAll") Integer mentionAll,
        @JsonProperty("recallStatus") Integer recallStatus,
        @JsonProperty("sentAt") LocalDateTime sentAt,
        @JsonProperty("updatedAt") LocalDateTime updatedAt
    ) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.senderAvatarUrl = senderAvatarUrl;
        this.messageType = messageType;
        this.content = content;
        this.fileId = fileId;
        this.replyToMessageId = replyToMessageId;
        this.replyPreview = replyPreview;
        this.mentionUserIds = mentionUserIds == null ? List.of() : mentionUserIds;
        this.fileName = fileName;
        this.fileMimeType = fileMimeType;
        this.fileSize = fileSize;
        this.mentionAll = mentionAll;
        this.recallStatus = recallStatus;
        this.sentAt = sentAt;
        this.updatedAt = updatedAt;
    }

    public GroupMessageResponse(
        GroupMessage message,
        User sender,
        FileAsset fileAsset,
        GroupMessage replyMessage,
        List<Long> mentionUserIds
    ) {
        this.messageId = message.getId();
        this.groupId = message.getGroupId();
        this.senderId = message.getSenderId();
        this.senderNickname = sender == null ? "" : sender.getNickname();
        this.senderAvatarUrl = sender == null ? null : sender.getAvatarUrl();
        this.messageType = message.getMessageType();
        this.content = message.getContent();
        this.fileId = message.getFileId();
        this.replyToMessageId = message.getReplyToMessageId();
        this.replyPreview = replyMessage == null ? null : buildReplyPreview(replyMessage);
        this.mentionUserIds = mentionUserIds == null ? List.of() : mentionUserIds;
        this.fileName = fileAsset == null ? null : fileAsset.getFileName();
        this.fileMimeType = fileAsset == null ? null : fileAsset.getMimeType();
        this.fileSize = fileAsset == null ? null : fileAsset.getFileSize();
        this.mentionAll = message.getMentionAll();
        this.recallStatus = message.getRecallStatus();
        this.sentAt = message.getSentAt();
        this.updatedAt = message.getUpdatedAt();
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public Long getFileId() {
        return fileId;
    }

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public String getReplyPreview() {
        return replyPreview;
    }

    public List<Long> getMentionUserIds() {
        return mentionUserIds;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getMentionAll() {
        return mentionAll;
    }

    public Integer getRecallStatus() {
        return recallStatus;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private String buildReplyPreview(GroupMessage replyMessage) {
        if (replyMessage.getRecallStatus() != null && replyMessage.getRecallStatus() == GroupMessage.RECALL_RECALLED) {
            return "Message recalled";
        }
        if (!"text".equals(replyMessage.getMessageType())) {
            return "[" + replyMessage.getMessageType() + "]";
        }
        String content = replyMessage.getContent() == null ? "" : replyMessage.getContent();
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }
}

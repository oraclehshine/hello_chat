package com.hellochat.backend.dto;

import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.PrivateMessage;
import java.time.LocalDateTime;

public class PrivateMessageResponse {

    private final Long messageId;
    private final Long chatId;
    private final Long senderId;
    private final String messageType;
    private final String content;
    private final Long fileId;
    private final String fileName;
    private final String fileMimeType;
    private final Long fileSize;
    private final Integer recallStatus;
    private final Integer messageStatus;
    private final LocalDateTime sentAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime pinnedAt;

    public PrivateMessageResponse(PrivateMessage message) {
        this(message, null);
    }

    public PrivateMessageResponse(PrivateMessage message, FileAsset fileAsset) {
        this.messageId = message.getId();
        this.chatId = message.getChatId();
        this.senderId = message.getSenderId();
        this.messageType = message.getMessageType();
        this.content = message.getContent();
        this.fileId = message.getFileId();
        this.fileName = fileAsset == null ? null : fileAsset.getFileName();
        this.fileMimeType = fileAsset == null ? null : fileAsset.getMimeType();
        this.fileSize = fileAsset == null ? null : fileAsset.getFileSize();
        this.recallStatus = message.getRecallStatus();
        this.messageStatus = message.getMessageStatus();
        this.sentAt = message.getSentAt();
        this.updatedAt = message.getUpdatedAt();
        this.pinnedAt = message.getPinnedAt();
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getSenderId() {
        return senderId;
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

    public String getFileName() {
        return fileName;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getRecallStatus() {
        return recallStatus;
    }

    public Integer getMessageStatus() {
        return messageStatus;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }
}

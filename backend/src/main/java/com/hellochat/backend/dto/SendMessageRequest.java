package com.hellochat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank
    private String messageType;

    @Size(max = 5000)
    private String content;

    private Long fileId;

    @Size(max = 255)
    private String fileName;

    private Long fileSize;

    private Boolean mentionAll;

    private Long replyToMessageId;

    private java.util.List<Long> mentionUserIds;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Boolean getMentionAll() {
        return mentionAll;
    }

    public void setMentionAll(Boolean mentionAll) {
        this.mentionAll = mentionAll;
    }

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Long replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public java.util.List<Long> getMentionUserIds() {
        return mentionUserIds;
    }

    public void setMentionUserIds(java.util.List<Long> mentionUserIds) {
        this.mentionUserIds = mentionUserIds;
    }
}

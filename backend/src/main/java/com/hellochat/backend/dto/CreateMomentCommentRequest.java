package com.hellochat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateMomentCommentRequest {

    @NotBlank
    @Size(max = 1000)
    private String content;

    private Long replyToCommentId;

    private java.util.List<Long> mentionUserIds;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getReplyToCommentId() {
        return replyToCommentId;
    }

    public void setReplyToCommentId(Long replyToCommentId) {
        this.replyToCommentId = replyToCommentId;
    }

    public java.util.List<Long> getMentionUserIds() {
        return mentionUserIds;
    }

    public void setMentionUserIds(java.util.List<Long> mentionUserIds) {
        this.mentionUserIds = mentionUserIds;
    }
}

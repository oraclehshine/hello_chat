package com.hellochat.backend.dto;

import com.hellochat.backend.entity.MomentComment;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class MomentCommentResponse {

    private final Long commentId;
    private final Long momentId;
    private final Long userId;
    private final String userNickname;
    private final String userAvatarUrl;
    private final Long replyToCommentId;
    private final String content;
    private final LocalDateTime createdAt;

    public MomentCommentResponse(MomentComment comment, User user) {
        this.commentId = comment.getId();
        this.momentId = comment.getMomentId();
        this.userId = comment.getUserId();
        this.userNickname = user == null ? "" : user.getNickname();
        this.userAvatarUrl = user == null ? null : user.getAvatarUrl();
        this.replyToCommentId = comment.getReplyToCommentId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getMomentId() {
        return momentId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public Long getReplyToCommentId() {
        return replyToCommentId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

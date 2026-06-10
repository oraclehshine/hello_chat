package com.hellochat.backend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hellochat.backend.entity.ChatGroup;
import java.time.LocalDateTime;

public class GroupResponse {

    private final Long groupId;
    private final Long ownerId;
    private final String groupName;
    private final String description;
    private final String avatarUrl;
    private final String notice;
    private final Integer status;
    private final String inviteCode;
    private final Integer chatEnabled;
    private final Integer recallLimitMinutes;
    private final long memberCount;
    private final long unreadCount;
    private final long mentionUnreadCount;
    private final boolean noticeUnread;
    private final LocalDateTime noticeUpdatedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public GroupResponse(ChatGroup group, long memberCount) {
        this(group, memberCount, 0, 0, false);
    }

    @JsonCreator
    public GroupResponse(
        @JsonProperty("groupId") Long groupId,
        @JsonProperty("ownerId") Long ownerId,
        @JsonProperty("groupName") String groupName,
        @JsonProperty("description") String description,
        @JsonProperty("avatarUrl") String avatarUrl,
        @JsonProperty("notice") String notice,
        @JsonProperty("status") Integer status,
        @JsonProperty("inviteCode") String inviteCode,
        @JsonProperty("chatEnabled") Integer chatEnabled,
        @JsonProperty("recallLimitMinutes") Integer recallLimitMinutes,
        @JsonProperty("memberCount") long memberCount,
        @JsonProperty("unreadCount") long unreadCount,
        @JsonProperty("mentionUnreadCount") long mentionUnreadCount,
        @JsonProperty("noticeUnread") boolean noticeUnread,
        @JsonProperty("noticeUpdatedAt") LocalDateTime noticeUpdatedAt,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("updatedAt") LocalDateTime updatedAt
    ) {
        this.groupId = groupId;
        this.ownerId = ownerId;
        this.groupName = groupName;
        this.description = description;
        this.avatarUrl = avatarUrl;
        this.notice = notice;
        this.status = status;
        this.inviteCode = inviteCode;
        this.chatEnabled = chatEnabled;
        this.recallLimitMinutes = recallLimitMinutes;
        this.memberCount = memberCount;
        this.unreadCount = unreadCount;
        this.mentionUnreadCount = mentionUnreadCount;
        this.noticeUnread = noticeUnread;
        this.noticeUpdatedAt = noticeUpdatedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public GroupResponse(ChatGroup group, long memberCount, long unreadCount, long mentionUnreadCount, boolean noticeUnread) {
        this.groupId = group.getId();
        this.ownerId = group.getOwnerId();
        this.groupName = group.getName();
        this.description = group.getDescription();
        this.avatarUrl = group.getAvatarUrl();
        this.notice = group.getNotice();
        this.status = group.getStatus();
        this.inviteCode = group.getInviteCode();
        this.chatEnabled = group.getChatEnabled();
        this.recallLimitMinutes = group.getRecallLimitMinutes();
        this.memberCount = memberCount;
        this.unreadCount = unreadCount;
        this.mentionUnreadCount = mentionUnreadCount;
        this.noticeUnread = noticeUnread;
        this.noticeUpdatedAt = group.getNoticeUpdatedAt();
        this.createdAt = group.getCreatedAt();
        this.updatedAt = group.getUpdatedAt();
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getNotice() {
        return notice;
    }

    public Integer getStatus() {
        return status;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public Integer getChatEnabled() {
        return chatEnabled;
    }

    public Integer getRecallLimitMinutes() {
        return recallLimitMinutes;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public long getMentionUnreadCount() {
        return mentionUnreadCount;
    }

    public boolean isNoticeUnread() {
        return noticeUnread;
    }

    public LocalDateTime getNoticeUpdatedAt() {
        return noticeUpdatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

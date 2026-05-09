package com.hellochat.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "groups")
public class ChatGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 64)
    private String name;

    private String description;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String notice;

    @Column(name = "notice_updated_at")
    private LocalDateTime noticeUpdatedAt;

    @Column(columnDefinition = "smallint default 1")
    private Integer status = 1;

    @Column(name = "max_member_count")
    private Integer maxMemberCount = 500;

    @Column(name = "invite_code")
    private String inviteCode;

    @Column(name = "chat_enabled", columnDefinition = "smallint default 1")
    private Integer chatEnabled = 1;

    @Column(name = "recall_limit_minutes")
    private Integer recallLimitMinutes = 2;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public LocalDateTime getNoticeUpdatedAt() {
        return noticeUpdatedAt;
    }

    public void setNoticeUpdatedAt(LocalDateTime noticeUpdatedAt) {
        this.noticeUpdatedAt = noticeUpdatedAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getMaxMemberCount() {
        return maxMemberCount;
    }

    public void setMaxMemberCount(Integer maxMemberCount) {
        this.maxMemberCount = maxMemberCount;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public Integer getChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(Integer chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public Integer getRecallLimitMinutes() {
        return recallLimitMinutes;
    }

    public void setRecallLimitMinutes(Integer recallLimitMinutes) {
        this.recallLimitMinutes = recallLimitMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

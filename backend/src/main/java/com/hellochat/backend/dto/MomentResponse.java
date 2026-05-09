package com.hellochat.backend.dto;

import com.hellochat.backend.entity.Moment;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class MomentResponse {

    private final Long momentId;
    private final Long authorId;
    private final String authorNickname;
    private final String authorAvatarUrl;
    private final String content;
    private final String location;
    private final List<String> tags;
    private final String mood;
    private final String activity;
    private final String auditStatus;
    private final String auditReason;
    private final String visibility;
    private final Integer likeCount;
    private final Integer commentCount;
    private final Integer collectCount;
    private final Integer viewCount;
    private final boolean liked;
    private final boolean collected;
    private final List<MomentMediaResponse> mediaList;
    private final List<Long> visibleUserIds;
    private final LocalDateTime editedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MomentResponse(
        Moment moment,
        User author,
        List<MomentMediaResponse> mediaList,
        List<Long> visibleUserIds,
        boolean liked,
        boolean collected
    ) {
        this.momentId = moment.getId();
        this.authorId = moment.getAuthorId();
        this.authorNickname = author == null ? "" : author.getNickname();
        this.authorAvatarUrl = author == null ? null : author.getAvatarUrl();
        this.content = moment.getContent();
        this.location = moment.getLocation();
        this.tags = parseTags(moment.getTags());
        this.mood = moment.getMood();
        this.activity = moment.getActivity();
        this.auditStatus = moment.getAuditStatus();
        this.auditReason = moment.getAuditReason();
        this.visibility = moment.getVisibility();
        this.likeCount = moment.getLikeCount();
        this.commentCount = moment.getCommentCount();
        this.collectCount = moment.getCollectCount();
        this.viewCount = moment.getViewCount();
        this.liked = liked;
        this.collected = collected;
        this.mediaList = mediaList;
        this.visibleUserIds = visibleUserIds;
        this.editedAt = moment.getEditedAt();
        this.createdAt = moment.getCreatedAt();
        this.updatedAt = moment.getUpdatedAt();
    }

    public Long getMomentId() {
        return momentId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getMood() {
        return mood;
    }

    public String getActivity() {
        return activity;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public String getAuditReason() {
        return auditReason;
    }

    public String getVisibility() {
        return visibility;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public Integer getCollectCount() {
        return collectCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public boolean isCollected() {
        return collected;
    }

    public List<MomentMediaResponse> getMediaList() {
        return mediaList;
    }

    public List<Long> getVisibleUserIds() {
        return visibleUserIds;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawTags.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
    }
}

package com.hellochat.backend.dto;

import com.hellochat.backend.entity.User;

public class MomentProfileSummaryResponse {

    private final Long userId;
    private final String nickname;
    private final String email;
    private final String avatarUrl;
    private final long momentCount;
    private final long totalLikeCount;
    private final long totalCommentCount;
    private final long totalCollectCount;
    private final long friendCount;
    private final long followerCount;
    private final long followingCount;

    public MomentProfileSummaryResponse(
        User user,
        long momentCount,
        long totalLikeCount,
        long totalCommentCount,
        long totalCollectCount,
        long friendCount
    ) {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.email = user.getEmail();
        this.avatarUrl = user.getAvatarUrl();
        this.momentCount = momentCount;
        this.totalLikeCount = totalLikeCount;
        this.totalCommentCount = totalCommentCount;
        this.totalCollectCount = totalCollectCount;
        this.friendCount = friendCount;
        this.followerCount = friendCount;
        this.followingCount = friendCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getMomentCount() {
        return momentCount;
    }

    public long getTotalLikeCount() {
        return totalLikeCount;
    }

    public long getTotalCommentCount() {
        return totalCommentCount;
    }

    public long getTotalCollectCount() {
        return totalCollectCount;
    }

    public long getFriendCount() {
        return friendCount;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }
}

package com.hellochat.backend.dto;

import com.hellochat.backend.entity.Friendship;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class FriendResponse {

    private Long friendshipId;
    private Long userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private String remarkName;
    private String friendGroup;
    private Integer star;
    private LocalDateTime createdAt;

    public FriendResponse(Friendship friendship, User friend) {
        this.friendshipId = friendship.getId();
        this.userId = friend.getId();
        this.email = friend.getEmail();
        this.nickname = friend.getNickname();
        this.avatarUrl = friend.getAvatarUrl();
        this.signature = friend.getSignature();
        this.remarkName = friendship.getRemarkName();
        this.friendGroup = friendship.getFriendGroup();
        this.star = friendship.getStar();
        this.createdAt = friendship.getCreatedAt();
    }

    public Long getFriendshipId() {
        return friendshipId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getSignature() {
        return signature;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public String getFriendGroup() {
        return friendGroup;
    }

    public Integer getStar() {
        return star;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

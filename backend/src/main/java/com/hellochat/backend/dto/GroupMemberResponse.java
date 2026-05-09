package com.hellochat.backend.dto;

import com.hellochat.backend.entity.GroupMember;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class GroupMemberResponse {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final String avatarUrl;
    private final Integer role;
    private final String groupNickname;
    private final LocalDateTime muteUntil;
    private final LocalDateTime noticeReadAt;
    private final LocalDateTime joinedAt;

    public GroupMemberResponse(GroupMember member, User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.avatarUrl = user.getAvatarUrl();
        this.role = member.getRole();
        this.groupNickname = member.getNickname();
        this.muteUntil = member.getMuteUntil();
        this.noticeReadAt = member.getNoticeReadAt();
        this.joinedAt = member.getJoinedAt();
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

    public Integer getRole() {
        return role;
    }

    public String getGroupNickname() {
        return groupNickname;
    }

    public LocalDateTime getMuteUntil() {
        return muteUntil;
    }

    public LocalDateTime getNoticeReadAt() {
        return noticeReadAt;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}

package com.hellochat.backend.dto;

import com.hellochat.backend.entity.GroupJoinRequest;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class GroupJoinRequestResponse {

    private final Long requestId;
    private final Long groupId;
    private final Long requesterId;
    private final String requesterEmail;
    private final String requesterNickname;
    private final String requesterAvatarUrl;
    private final String message;
    private final Integer status;
    private final LocalDateTime createdAt;

    public GroupJoinRequestResponse(GroupJoinRequest request, User requester) {
        this.requestId = request.getId();
        this.groupId = request.getGroupId();
        this.requesterId = request.getRequesterId();
        this.requesterEmail = requester == null ? "" : requester.getEmail();
        this.requesterNickname = requester == null ? "" : requester.getNickname();
        this.requesterAvatarUrl = requester == null ? null : requester.getAvatarUrl();
        this.message = request.getMessage();
        this.status = request.getStatus();
        this.createdAt = request.getCreatedAt();
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getRequesterNickname() {
        return requesterNickname;
    }

    public String getRequesterAvatarUrl() {
        return requesterAvatarUrl;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

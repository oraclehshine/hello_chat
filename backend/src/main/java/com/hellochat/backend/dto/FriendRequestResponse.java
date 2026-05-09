package com.hellochat.backend.dto;

import com.hellochat.backend.entity.FriendRequest;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class FriendRequestResponse {

    private Long requestId;
    private Long requesterId;
    private String requesterEmail;
    private String requesterNickname;
    private String requesterAvatarUrl;
    private Long receiverId;
    private String receiverEmail;
    private String receiverNickname;
    private String receiverAvatarUrl;
    private String remark;
    private Integer status;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    public FriendRequestResponse(FriendRequest request, User requester, User receiver) {
        this.requestId = request.getId();
        this.requesterId = request.getRequesterId();
        this.requesterEmail = requester == null ? null : requester.getEmail();
        this.requesterNickname = requester == null ? null : requester.getNickname();
        this.requesterAvatarUrl = requester == null ? null : requester.getAvatarUrl();
        this.receiverId = request.getReceiverId();
        this.receiverEmail = receiver == null ? null : receiver.getEmail();
        this.receiverNickname = receiver == null ? null : receiver.getNickname();
        this.receiverAvatarUrl = receiver == null ? null : receiver.getAvatarUrl();
        this.remark = request.getRemark();
        this.status = request.getStatus();
        this.handledAt = request.getHandledAt();
        this.createdAt = request.getCreatedAt();
    }

    public Long getRequestId() {
        return requestId;
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

    public Long getReceiverId() {
        return receiverId;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }

    public String getReceiverAvatarUrl() {
        return receiverAvatarUrl;
    }

    public String getRemark() {
        return remark;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

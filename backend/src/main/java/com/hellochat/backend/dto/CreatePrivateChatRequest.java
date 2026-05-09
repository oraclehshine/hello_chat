package com.hellochat.backend.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePrivateChatRequest {

    @NotNull
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }
}

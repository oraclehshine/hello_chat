package com.hellochat.backend.dto;

import com.hellochat.backend.entity.UserPresence;
import java.time.LocalDateTime;

public class PresenceResponse {

    private final Long userId;
    private final String status;
    private final LocalDateTime lastActiveAt;
    private final LocalDateTime updatedAt;

    public PresenceResponse(UserPresence presence) {
        this.userId = presence.getUserId();
        this.status = presence.getStatus();
        this.lastActiveAt = presence.getLastActiveAt();
        this.updatedAt = presence.getUpdatedAt();
    }

    public Long getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

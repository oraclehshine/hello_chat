package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateGroupRequest {

    @Size(min = 3, max = 30, message = "groupName length must be 3-30")
    private String groupName;

    @Size(max = 255, message = "description length must not exceed 255")
    private String description;

    private String avatarUrl;

    private Boolean chatEnabled;

    private Integer recallLimitMinutes;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
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

    public Boolean getChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(Boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public Integer getRecallLimitMinutes() {
        return recallLimitMinutes;
    }

    public void setRecallLimitMinutes(Integer recallLimitMinutes) {
        this.recallLimitMinutes = recallLimitMinutes;
    }
}

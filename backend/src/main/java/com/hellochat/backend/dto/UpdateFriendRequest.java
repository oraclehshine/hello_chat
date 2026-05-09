package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateFriendRequest {

    @Size(max = 64)
    private String remarkName;

    @Size(max = 64)
    private String friendGroup;

    private Boolean star;

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(String friendGroup) {
        this.friendGroup = friendGroup;
    }

    public Boolean getStar() {
        return star;
    }

    public void setStar(Boolean star) {
        this.star = star;
    }
}

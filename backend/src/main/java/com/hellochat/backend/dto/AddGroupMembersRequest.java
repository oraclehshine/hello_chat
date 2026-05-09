package com.hellochat.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class AddGroupMembersRequest {

    @NotEmpty(message = "memberIds is required")
    private List<Long> memberIds;

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}

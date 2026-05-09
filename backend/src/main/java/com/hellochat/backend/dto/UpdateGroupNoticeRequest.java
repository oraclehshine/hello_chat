package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateGroupNoticeRequest {

    @Size(max = 1000, message = "notice length must not exceed 1000")
    private String notice;

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }
}

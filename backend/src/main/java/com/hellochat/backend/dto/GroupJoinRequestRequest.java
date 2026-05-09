package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;

public class GroupJoinRequestRequest {

    @Size(max = 255)
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

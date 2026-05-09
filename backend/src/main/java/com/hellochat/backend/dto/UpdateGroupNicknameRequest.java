package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;

public class UpdateGroupNicknameRequest {

    @Size(max = 64)
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

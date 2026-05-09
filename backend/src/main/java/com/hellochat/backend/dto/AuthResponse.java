package com.hellochat.backend.dto;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final Long userId;
    private final String email;
    private final String nickname;
    private final String avatarUrl;

    public AuthResponse(String accessToken, String refreshToken, Long userId, String email, String nickname, String avatarUrl) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}

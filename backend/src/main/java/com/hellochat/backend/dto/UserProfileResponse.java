package com.hellochat.backend.dto;

import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class UserProfileResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer gender;
    private Integer age;
    private String phone;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public UserProfileResponse(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.avatarUrl = user.getAvatarUrl();
        this.signature = user.getSignature();
        this.gender = user.getGender();
        this.age = user.getAge();
        this.phone = user.getPhone();
        this.status = user.getStatus().name();
        this.lastLoginAt = user.getLastLoginAt();
        this.createdAt = user.getCreatedAt();
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

    public String getSignature() {
        return signature;
    }

    public Integer getGender() {
        return gender;
    }

    public Integer getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

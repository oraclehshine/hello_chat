package com.hellochat.backend.dto;

import java.util.List;

public class SocialSearchResponse {

    private final List<UserProfileResponse> users;
    private final List<GroupResponse> groups;

    public SocialSearchResponse(List<UserProfileResponse> users, List<GroupResponse> groups) {
        this.users = users;
        this.groups = groups;
    }

    public List<UserProfileResponse> getUsers() {
        return users;
    }

    public List<GroupResponse> getGroups() {
        return groups;
    }
}

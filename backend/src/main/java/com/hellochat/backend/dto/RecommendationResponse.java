package com.hellochat.backend.dto;

import java.util.List;

public class RecommendationResponse {

    private final List<UserProfileResponse> friends;
    private final List<GroupResponse> groups;
    private final List<TopicRecommendationResponse> topics;

    public RecommendationResponse(
        List<UserProfileResponse> friends,
        List<GroupResponse> groups,
        List<TopicRecommendationResponse> topics
    ) {
        this.friends = friends;
        this.groups = groups;
        this.topics = topics;
    }

    public List<UserProfileResponse> getFriends() {
        return friends;
    }

    public List<GroupResponse> getGroups() {
        return groups;
    }

    public List<TopicRecommendationResponse> getTopics() {
        return topics;
    }
}

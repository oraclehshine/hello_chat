package com.hellochat.backend.dto;

public class TopicRecommendationResponse {

    private final String tag;
    private final long momentCount;

    public TopicRecommendationResponse(String tag, long momentCount) {
        this.tag = tag;
        this.momentCount = momentCount;
    }

    public String getTag() {
        return tag;
    }

    public long getMomentCount() {
        return momentCount;
    }
}

package com.hellochat.backend.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateMomentRequest {

    @Size(max = 5000)
    private String content;

    private List<Long> fileIds;

    @Size(max = 128)
    private String location;

    private List<String> tags;

    @Size(max = 32)
    private String mood;

    @Size(max = 32)
    private String activity;

    private String visibility = "public";

    private List<Long> visibleUserIds;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<Long> getVisibleUserIds() {
        return visibleUserIds;
    }

    public void setVisibleUserIds(List<Long> visibleUserIds) {
        this.visibleUserIds = visibleUserIds;
    }
}

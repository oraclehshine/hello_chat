package com.hellochat.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class MuteGroupMemberRequest {

    @Min(value = 1, message = "minutes must be at least 1")
    @Max(value = 43200, message = "minutes must not exceed 43200")
    private int minutes = 10;

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }
}

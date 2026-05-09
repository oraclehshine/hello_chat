package com.hellochat.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ReviewMomentReportRequest {

    @Min(2)
    @Max(3)
    private Integer status;

    @Size(max = 255)
    private String handleNote;

    private Boolean deleteMoment;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public Boolean getDeleteMoment() {
        return deleteMoment;
    }

    public void setDeleteMoment(Boolean deleteMoment) {
        this.deleteMoment = deleteMoment;
    }
}

package com.hellochat.backend.dto;

import com.hellochat.backend.entity.Moment;
import com.hellochat.backend.entity.MomentReport;
import com.hellochat.backend.entity.User;
import java.time.LocalDateTime;

public class MomentReportResponse {

    private final Long reportId;
    private final Long momentId;
    private final Long reporterId;
    private final String reporterNickname;
    private final String authorNickname;
    private final String momentContent;
    private final String reason;
    private final Integer status;
    private final Long handledBy;
    private final String handleNote;
    private final LocalDateTime handledAt;
    private final LocalDateTime createdAt;

    public MomentReportResponse(MomentReport report, Moment moment, User reporter, User author) {
        this.reportId = report.getId();
        this.momentId = report.getMomentId();
        this.reporterId = report.getReporterId();
        this.reporterNickname = reporter == null ? "" : reporter.getNickname();
        this.authorNickname = author == null ? "" : author.getNickname();
        this.momentContent = moment == null ? "" : moment.getContent();
        this.reason = report.getReason();
        this.status = report.getStatus();
        this.handledBy = report.getHandledBy();
        this.handleNote = report.getHandleNote();
        this.handledAt = report.getHandledAt();
        this.createdAt = report.getCreatedAt();
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getMomentId() {
        return momentId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public String getReporterNickname() {
        return reporterNickname;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getMomentContent() {
        return momentContent;
    }

    public String getReason() {
        return reason;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.hellochat.backend.controller;

import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.CreateMomentCommentRequest;
import com.hellochat.backend.dto.CreateMomentRequest;
import com.hellochat.backend.dto.MomentCommentResponse;
import com.hellochat.backend.dto.MomentProfileSummaryResponse;
import com.hellochat.backend.dto.MomentResponse;
import com.hellochat.backend.dto.MomentReportResponse;
import com.hellochat.backend.dto.NotificationResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.ReportMomentRequest;
import com.hellochat.backend.dto.ReviewMomentReportRequest;
import com.hellochat.backend.dto.UpdateMomentRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.service.MomentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moments")
public class MomentController {

    private final MomentService momentService;
    private final TokenProvider tokenProvider;

    public MomentController(MomentService momentService, TokenProvider tokenProvider) {
        this.momentService = momentService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping
    public ApiResponse<MomentResponse> createMoment(
        HttpServletRequest request,
        @Valid @RequestBody CreateMomentRequest createMomentRequest
    ) {
        return ApiResponse.success(momentService.createMoment(currentUserId(request), createMomentRequest));
    }

    @GetMapping
    public ApiResponse<PageResponse<MomentResponse>> listMoments(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(momentService.listMoments(currentUserId(request), page, pageSize));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<PageResponse<MomentResponse>> listUserMoments(
        HttpServletRequest request,
        @PathVariable Long userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(momentService.listUserMoments(currentUserId(request), userId, page, pageSize));
    }

    @GetMapping("/users/{userId}/summary")
    public ApiResponse<MomentProfileSummaryResponse> getProfileSummary(
        HttpServletRequest request,
        @PathVariable Long userId
    ) {
        return ApiResponse.success(momentService.getProfileSummary(currentUserId(request), userId));
    }

    @GetMapping("/collections")
    public ApiResponse<PageResponse<MomentResponse>> listCollectedMoments(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(momentService.listCollectedMoments(currentUserId(request), page, pageSize));
    }

    @GetMapping("/{momentId}")
    public ApiResponse<MomentResponse> getMoment(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.getMoment(currentUserId(request), momentId));
    }

    @PutMapping("/{momentId}")
    public ApiResponse<MomentResponse> updateMoment(
        HttpServletRequest request,
        @PathVariable Long momentId,
        @Valid @RequestBody UpdateMomentRequest updateMomentRequest
    ) {
        return ApiResponse.success(momentService.updateMoment(currentUserId(request), momentId, updateMomentRequest));
    }

    @DeleteMapping("/{momentId}")
    public ApiResponse<Void> deleteMoment(HttpServletRequest request, @PathVariable Long momentId) {
        momentService.deleteMoment(currentUserId(request), momentId);
        return ApiResponse.success();
    }

    @PostMapping("/{momentId}/likes")
    public ApiResponse<MomentResponse> likeMoment(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.likeMoment(currentUserId(request), momentId));
    }

    @DeleteMapping("/{momentId}/likes")
    public ApiResponse<MomentResponse> unlikeMoment(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.unlikeMoment(currentUserId(request), momentId));
    }

    @GetMapping("/{momentId}/likes")
    public ApiResponse<List<UserProfileResponse>> listLikes(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.listLikes(currentUserId(request), momentId));
    }

    @PostMapping("/{momentId}/comments")
    public ApiResponse<MomentCommentResponse> addComment(
        HttpServletRequest request,
        @PathVariable Long momentId,
        @Valid @RequestBody CreateMomentCommentRequest createMomentCommentRequest
    ) {
        return ApiResponse.success(momentService.addComment(currentUserId(request), momentId, createMomentCommentRequest));
    }

    @GetMapping("/{momentId}/comments")
    public ApiResponse<List<MomentCommentResponse>> listComments(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.listComments(currentUserId(request), momentId));
    }

    @DeleteMapping("/{momentId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
        HttpServletRequest request,
        @PathVariable Long momentId,
        @PathVariable Long commentId
    ) {
        momentService.deleteComment(currentUserId(request), momentId, commentId);
        return ApiResponse.success();
    }

    @PostMapping("/{momentId}/collect")
    public ApiResponse<MomentResponse> collectMoment(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.collectMoment(currentUserId(request), momentId));
    }

    @DeleteMapping("/{momentId}/collect")
    public ApiResponse<MomentResponse> uncollectMoment(HttpServletRequest request, @PathVariable Long momentId) {
        return ApiResponse.success(momentService.uncollectMoment(currentUserId(request), momentId));
    }

    @PostMapping("/{momentId}/report")
    public ApiResponse<Void> reportMoment(
        HttpServletRequest request,
        @PathVariable Long momentId,
        @Valid @RequestBody ReportMomentRequest reportMomentRequest
    ) {
        momentService.reportMoment(currentUserId(request), momentId, reportMomentRequest);
        return ApiResponse.success();
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<MomentReportResponse>> listMomentReports(
        HttpServletRequest request,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(momentService.listMomentReports(currentUserId(request), status, page, pageSize));
    }

    @PutMapping("/reports/{reportId}/review")
    public ApiResponse<MomentReportResponse> reviewMomentReport(
        HttpServletRequest request,
        @PathVariable Long reportId,
        @Valid @RequestBody ReviewMomentReportRequest reviewMomentReportRequest
    ) {
        return ApiResponse.success(momentService.reviewMomentReport(currentUserId(request), reportId, reviewMomentReportRequest));
    }

    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
        HttpServletRequest request,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(momentService.listNotifications(currentUserId(request), page, pageSize));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> countUnreadNotifications(HttpServletRequest request) {
        return ApiResponse.success(momentService.countUnreadNotifications(currentUserId(request)));
    }

    @PutMapping("/notifications/{notificationId}/read")
    public ApiResponse<Void> markNotificationRead(HttpServletRequest request, @PathVariable Long notificationId) {
        momentService.markNotificationRead(currentUserId(request), notificationId);
        return ApiResponse.success();
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Void> markAllNotificationsRead(HttpServletRequest request) {
        momentService.markAllNotificationsRead(currentUserId(request));
        return ApiResponse.success();
    }

    private Long currentUserId(HttpServletRequest request) {
        return CurrentUser.requireUserId(request, tokenProvider);
    }
}

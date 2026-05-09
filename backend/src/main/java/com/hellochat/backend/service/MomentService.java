package com.hellochat.backend.service;

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
import java.util.List;

public interface MomentService {

    MomentResponse createMoment(Long userId, CreateMomentRequest request);

    PageResponse<MomentResponse> listMoments(Long userId, int page, int pageSize);

    PageResponse<MomentResponse> listUserMoments(Long userId, Long authorId, int page, int pageSize);

    MomentProfileSummaryResponse getProfileSummary(Long userId, Long profileUserId);

    PageResponse<MomentResponse> listCollectedMoments(Long userId, int page, int pageSize);

    MomentResponse getMoment(Long userId, Long momentId);

    MomentResponse updateMoment(Long userId, Long momentId, UpdateMomentRequest request);

    void deleteMoment(Long userId, Long momentId);

    MomentResponse likeMoment(Long userId, Long momentId);

    MomentResponse unlikeMoment(Long userId, Long momentId);

    List<UserProfileResponse> listLikes(Long userId, Long momentId);

    MomentCommentResponse addComment(Long userId, Long momentId, CreateMomentCommentRequest request);

    List<MomentCommentResponse> listComments(Long userId, Long momentId);

    void deleteComment(Long userId, Long momentId, Long commentId);

    MomentResponse collectMoment(Long userId, Long momentId);

    MomentResponse uncollectMoment(Long userId, Long momentId);

    void reportMoment(Long userId, Long momentId, ReportMomentRequest request);

    PageResponse<MomentReportResponse> listMomentReports(Long userId, Integer status, int page, int pageSize);

    MomentReportResponse reviewMomentReport(Long userId, Long reportId, ReviewMomentReportRequest request);

    PageResponse<NotificationResponse> listNotifications(Long userId, int page, int pageSize);

    long countUnreadNotifications(Long userId);

    void markNotificationRead(Long userId, Long notificationId);

    void markAllNotificationsRead(Long userId);
}

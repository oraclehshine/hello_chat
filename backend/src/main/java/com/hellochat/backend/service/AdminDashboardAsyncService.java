package com.hellochat.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardAsyncService {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardAsyncService(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Async("eventDispatchExecutor")
    public void recordPrivateMessage(Long chatId, Long senderId, String messageType, String content) {
        adminDashboardService.recordPrivateMessage(chatId, senderId, messageType, content);
    }

    @Async("eventDispatchExecutor")
    public void recordGroupMessage(Long groupId, Long senderId, String messageType, String content) {
        adminDashboardService.recordGroupMessage(groupId, senderId, messageType, content);
    }

    @Async("eventDispatchExecutor")
    public void recordGroupCreated(Long groupId, Long ownerId, String groupName) {
        adminDashboardService.recordGroupCreated(groupId, ownerId, groupName);
    }

    @Async("eventDispatchExecutor")
    public void recordMomentCreated(Long momentId, Long authorId, String content) {
        adminDashboardService.recordMomentCreated(momentId, authorId, content);
    }

    @Async("eventDispatchExecutor")
    public void recordMomentReported(Long momentId, Long reporterId) {
        adminDashboardService.recordMomentReported(momentId, reporterId);
    }

    @Async("eventDispatchExecutor")
    public void recordMomentReportReviewed(Long reportId, Long reviewerId, Integer status) {
        adminDashboardService.recordMomentReportReviewed(reportId, reviewerId, status);
    }

    @Async("eventDispatchExecutor")
    public void recordPresence(Long userId, String status) {
        adminDashboardService.recordPresence(userId, status);
    }

    @Async("eventDispatchExecutor")
    public void recordFileUpload(Long uploaderId, Long fileId, String scene, long fileSize) {
        adminDashboardService.recordFileUpload(uploaderId, fileId, scene, fileSize);
    }
}

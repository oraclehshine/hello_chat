package com.hellochat.backend.service.impl;

import com.hellochat.backend.dto.CreateMomentCommentRequest;
import com.hellochat.backend.dto.CreateMomentRequest;
import com.hellochat.backend.dto.MomentCommentResponse;
import com.hellochat.backend.dto.MomentMediaResponse;
import com.hellochat.backend.dto.MomentProfileSummaryResponse;
import com.hellochat.backend.dto.MomentResponse;
import com.hellochat.backend.dto.MomentReportResponse;
import com.hellochat.backend.dto.NotificationResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.ReportMomentRequest;
import com.hellochat.backend.dto.ReviewMomentReportRequest;
import com.hellochat.backend.dto.UpdateMomentRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.Moment;
import com.hellochat.backend.entity.MomentCollect;
import com.hellochat.backend.entity.MomentComment;
import com.hellochat.backend.entity.MomentLike;
import com.hellochat.backend.entity.MomentMedia;
import com.hellochat.backend.entity.MomentReport;
import com.hellochat.backend.entity.MomentVisibleUser;
import com.hellochat.backend.entity.Notification;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.repository.FileAssetRepository;
import com.hellochat.backend.repository.FriendshipRepository;
import com.hellochat.backend.repository.MomentCollectRepository;
import com.hellochat.backend.repository.MomentCommentRepository;
import com.hellochat.backend.repository.MomentLikeRepository;
import com.hellochat.backend.repository.MomentMediaRepository;
import com.hellochat.backend.repository.MomentRepository;
import com.hellochat.backend.repository.MomentReportRepository;
import com.hellochat.backend.repository.MomentVisibleUserRepository;
import com.hellochat.backend.repository.NotificationRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.MomentService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MomentServiceImpl implements MomentService {

    private static final Set<String> VISIBILITIES = Set.of("public", "friends", "private", "specified");
    private static final Set<String> BLOCKED_CONTENT_WORDS = Set.of("spam", "abuse", "illegal");
    private static final int MAX_TAG_COUNT = 10;
    private static final int MAX_IMAGE_COUNT = 9;
    private static final int MAX_VIDEO_COUNT = 3;

    @Value("${hello-chat.moderation.admin-user-ids:1}")
    private Set<Long> moderationAdminUserIds;

    private final MomentRepository momentRepository;
    private final MomentMediaRepository momentMediaRepository;
    private final MomentCommentRepository momentCommentRepository;
    private final MomentLikeRepository momentLikeRepository;
    private final MomentCollectRepository momentCollectRepository;
    private final MomentReportRepository momentReportRepository;
    private final MomentVisibleUserRepository momentVisibleUserRepository;
    private final NotificationRepository notificationRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public MomentServiceImpl(
        MomentRepository momentRepository,
        MomentMediaRepository momentMediaRepository,
        MomentCommentRepository momentCommentRepository,
        MomentLikeRepository momentLikeRepository,
        MomentCollectRepository momentCollectRepository,
        MomentReportRepository momentReportRepository,
        MomentVisibleUserRepository momentVisibleUserRepository,
        NotificationRepository notificationRepository,
        FileAssetRepository fileAssetRepository,
        UserRepository userRepository,
        FriendshipRepository friendshipRepository
    ) {
        this.momentRepository = momentRepository;
        this.momentMediaRepository = momentMediaRepository;
        this.momentCommentRepository = momentCommentRepository;
        this.momentLikeRepository = momentLikeRepository;
        this.momentCollectRepository = momentCollectRepository;
        this.momentReportRepository = momentReportRepository;
        this.momentVisibleUserRepository = momentVisibleUserRepository;
        this.notificationRepository = notificationRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Override
    @Transactional
    public MomentResponse createMoment(Long userId, CreateMomentRequest request) {
        requireUser(userId);
        List<Long> fileIds = safeFileIds(request.getFileIds());
        if ((request.getContent() == null || request.getContent().isBlank()) && fileIds.isEmpty()) {
            throw new IllegalArgumentException("moment content or media is required");
        }
        validateVisibility(request.getVisibility());
        validateFiles(userId, fileIds);
        List<Long> visibleUserIds = safeVisibleUserIds(request.getVisibleUserIds());
        List<String> tags = normalizeTags(request.getTags(), request.getContent());
        String visibility = request.getVisibility() == null ? "public" : request.getVisibility();
        validateSpecifiedVisibility(userId, visibility, visibleUserIds);
        validateContentAudit(request.getContent());

        Moment moment = new Moment();
        moment.setAuthorId(userId);
        moment.setContent(request.getContent() == null ? "" : request.getContent().trim());
        moment.setLocation(request.getLocation());
        moment.setTags(String.join(",", tags));
        moment.setMood(cleanShortText(request.getMood()));
        moment.setActivity(cleanShortText(request.getActivity()));
        moment.setAuditStatus("approved");
        moment.setVisibility(visibility);
        moment.setCreatedAt(LocalDateTime.now());
        moment.setUpdatedAt(LocalDateTime.now());
        Moment saved = momentRepository.save(moment);
        saveMedia(saved.getId(), fileIds);
        saveVisibleUsers(saved.getId(), visibleUserIds);
        return toMomentResponse(userId, saved);
    }

    @Override
    public PageResponse<MomentResponse> listMoments(Long userId, int page, int pageSize) {
        requireUser(userId);
        Page<Moment> result = momentRepository.findVisibleMoments(userId, pageRequest(page, pageSize));
        return toMomentPage(userId, result, page, pageSize);
    }

    @Override
    public PageResponse<MomentResponse> listUserMoments(Long userId, Long authorId, int page, int pageSize) {
        requireUser(userId);
        requireUser(authorId);
        Page<Moment> result = momentRepository.findVisibleUserMoments(
            userId,
            authorId,
            pageRequest(page, pageSize)
        );
        return toMomentPage(userId, result, page, pageSize);
    }

    @Override
    public MomentProfileSummaryResponse getProfileSummary(Long userId, Long profileUserId) {
        requireUser(userId);
        User profileUser = requireUser(profileUserId);
        MomentRepository.MomentProfileSummaryProjection summary = momentRepository.summarizeByAuthorId(profileUserId);
        long friendCount = friendshipRepository
            .findByUserAIdAndDeletedAtIsNullOrUserBIdAndDeletedAtIsNullOrderByCreatedAtDesc(profileUserId, profileUserId)
            .size();
        return new MomentProfileSummaryResponse(
            profileUser,
            summary.getMomentCount(),
            summary.getTotalLikeCount(),
            summary.getTotalCommentCount(),
            summary.getTotalCollectCount(),
            friendCount
        );
    }

    @Override
    public PageResponse<MomentResponse> listCollectedMoments(Long userId, int page, int pageSize) {
        requireUser(userId);
        Page<Moment> result = momentRepository.findVisibleCollectedMoments(userId, pageRequest(page, pageSize));
        return toMomentPage(userId, result, page, pageSize);
    }

    @Override
    @Transactional
    public MomentResponse getMoment(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        moment.setViewCount(nullToZero(moment.getViewCount()) + 1);
        momentRepository.save(moment);
        return toMomentResponse(userId, moment);
    }

    @Override
    @Transactional
    public MomentResponse updateMoment(Long userId, Long momentId, UpdateMomentRequest request) {
        Moment moment = requireOwnedMoment(userId, momentId);
        if (moment.getCreatedAt().isBefore(LocalDateTime.now().minusHours(2))) {
            throw new IllegalArgumentException("moment edit window expired");
        }
        if (request.getVisibility() != null) {
            validateVisibility(request.getVisibility());
            moment.setVisibility(request.getVisibility());
        }
        if (request.getContent() != null) {
            validateContentAudit(request.getContent());
            moment.setContent(request.getContent().trim());
            if (request.getTags() == null) {
                moment.setTags(String.join(",", normalizeTags(null, request.getContent())));
            }
        }
        if (request.getLocation() != null) {
            moment.setLocation(request.getLocation());
        }
        if (request.getTags() != null) {
            moment.setTags(String.join(",", normalizeTags(request.getTags(), moment.getContent())));
        }
        if (request.getMood() != null) {
            moment.setMood(cleanShortText(request.getMood()));
        }
        if (request.getActivity() != null) {
            moment.setActivity(cleanShortText(request.getActivity()));
        }
        if (request.getFileIds() != null) {
            List<Long> fileIds = safeFileIds(request.getFileIds());
            validateFiles(userId, fileIds);
            momentMediaRepository.deleteByMomentId(momentId);
            saveMedia(momentId, fileIds);
        }
        if (request.getVisibility() != null || request.getVisibleUserIds() != null) {
            List<Long> visibleUserIds = safeVisibleUserIds(request.getVisibleUserIds());
            validateSpecifiedVisibility(userId, moment.getVisibility(), visibleUserIds);
            momentVisibleUserRepository.deleteByMomentId(momentId);
            saveVisibleUsers(momentId, visibleUserIds);
        }
        moment.setEditedAt(LocalDateTime.now());
        moment.setUpdatedAt(LocalDateTime.now());
        return toMomentResponse(userId, momentRepository.save(moment));
    }

    @Override
    @Transactional
    public void deleteMoment(Long userId, Long momentId) {
        Moment moment = requireOwnedMoment(userId, momentId);
        moment.setDeletedAt(LocalDateTime.now());
        moment.setUpdatedAt(LocalDateTime.now());
        momentRepository.save(moment);
    }

    @Override
    @Transactional
    public MomentResponse likeMoment(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        if (momentLikeRepository.findByMomentIdAndUserId(momentId, userId).isEmpty()) {
            MomentLike like = new MomentLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            momentLikeRepository.save(like);
            moment.setLikeCount(nullToZero(moment.getLikeCount()) + 1);
            moment.setUpdatedAt(LocalDateTime.now());
            momentRepository.save(moment);
            if (!moment.getAuthorId().equals(userId)) {
                saveNotification(moment.getAuthorId(), "moment_like", "New like", "Someone liked your moment", momentId);
            }
        }
        return toMomentResponse(userId, moment);
    }

    @Override
    @Transactional
    public MomentResponse unlikeMoment(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        momentLikeRepository.findByMomentIdAndUserId(momentId, userId).ifPresent(like -> {
            momentLikeRepository.delete(like);
            moment.setLikeCount(Math.max(0, nullToZero(moment.getLikeCount()) - 1));
            moment.setUpdatedAt(LocalDateTime.now());
            momentRepository.save(moment);
        });
        return toMomentResponse(userId, moment);
    }

    @Override
    public List<UserProfileResponse> listLikes(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        List<Long> userIds = momentLikeRepository.findByMomentIdOrderByCreatedAtAsc(momentId)
            .stream()
            .map(MomentLike::getUserId)
            .toList();
        return userRepository.findAllById(userIds).stream().map(UserProfileResponse::new).toList();
    }

    @Override
    @Transactional
    public MomentCommentResponse addComment(Long userId, Long momentId, CreateMomentCommentRequest request) {
        User user = requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        if (request.getReplyToCommentId() != null) {
            MomentComment reply = momentCommentRepository.findById(request.getReplyToCommentId())
                .orElseThrow(() -> new IllegalArgumentException("comment not found"));
            if (!reply.getMomentId().equals(momentId) || reply.getDeletedAt() != null) {
                throw new IllegalArgumentException("comment not found");
            }
        }
        MomentComment comment = new MomentComment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setReplyToCommentId(request.getReplyToCommentId());
        comment.setContent(request.getContent().trim());
        comment.setCreatedAt(LocalDateTime.now());
        MomentComment saved = momentCommentRepository.save(comment);
        moment.setCommentCount(nullToZero(moment.getCommentCount()) + 1);
        moment.setUpdatedAt(LocalDateTime.now());
        momentRepository.save(moment);
        if (!moment.getAuthorId().equals(userId)) {
            saveNotification(moment.getAuthorId(), "moment_comment", "New comment", comment.getContent(), momentId);
        }
        if (request.getMentionUserIds() != null) {
            new LinkedHashSet<>(request.getMentionUserIds()).forEach(mentionedUserId -> {
                requireUser(mentionedUserId);
                if (!mentionedUserId.equals(userId)) {
                    saveNotification(mentionedUserId, "moment_mention", "You were mentioned", comment.getContent(), momentId);
                }
            });
        }
        return new MomentCommentResponse(saved, user);
    }

    @Override
    public List<MomentCommentResponse> listComments(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        List<MomentComment> comments = momentCommentRepository.findByMomentIdAndDeletedAtIsNullOrderByCreatedAtAsc(momentId);
        Map<Long, User> users = userRepository.findAllById(comments.stream().map(MomentComment::getUserId).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return comments.stream().map(comment -> new MomentCommentResponse(comment, users.get(comment.getUserId()))).toList();
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long momentId, Long commentId) {
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        MomentComment comment = momentCommentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("comment not found"));
        if (!comment.getMomentId().equals(momentId) || comment.getDeletedAt() != null) {
            throw new IllegalArgumentException("comment not found");
        }
        if (!comment.getUserId().equals(userId) && !moment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
        comment.setDeletedAt(LocalDateTime.now());
        momentCommentRepository.save(comment);
        moment.setCommentCount(Math.max(0, nullToZero(moment.getCommentCount()) - 1));
        moment.setUpdatedAt(LocalDateTime.now());
        momentRepository.save(moment);
    }

    @Override
    @Transactional
    public MomentResponse collectMoment(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        if (momentCollectRepository.findByMomentIdAndUserId(momentId, userId).isEmpty()) {
            MomentCollect collect = new MomentCollect();
            collect.setMomentId(momentId);
            collect.setUserId(userId);
            collect.setCreatedAt(LocalDateTime.now());
            momentCollectRepository.save(collect);
            moment.setCollectCount(nullToZero(moment.getCollectCount()) + 1);
            moment.setUpdatedAt(LocalDateTime.now());
            momentRepository.save(moment);
            if (!moment.getAuthorId().equals(userId)) {
                saveNotification(moment.getAuthorId(), "moment_collect", "New collect", "Someone collected your moment", momentId);
            }
        }
        return toMomentResponse(userId, moment);
    }

    @Override
    @Transactional
    public MomentResponse uncollectMoment(Long userId, Long momentId) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        momentCollectRepository.findByMomentIdAndUserId(momentId, userId).ifPresent(collect -> {
            momentCollectRepository.delete(collect);
            moment.setCollectCount(Math.max(0, nullToZero(moment.getCollectCount()) - 1));
            moment.setUpdatedAt(LocalDateTime.now());
            momentRepository.save(moment);
        });
        return toMomentResponse(userId, moment);
    }

    @Override
    @Transactional
    public void reportMoment(Long userId, Long momentId, ReportMomentRequest request) {
        requireUser(userId);
        Moment moment = requireMoment(momentId);
        requireCanViewMoment(userId, moment);
        if (momentReportRepository.existsByMomentIdAndReporterId(momentId, userId)) {
            throw new IllegalArgumentException("moment already reported");
        }
        MomentReport report = new MomentReport();
        report.setMomentId(momentId);
        report.setReporterId(userId);
        report.setReason(request.getReason().trim());
        report.setStatus(1);
        report.setCreatedAt(LocalDateTime.now());
        momentReportRepository.save(report);
    }

    @Override
    public PageResponse<MomentReportResponse> listMomentReports(Long userId, Integer status, int page, int pageSize) {
        requireModerator(userId);
        Page<MomentReport> result = status == null
            ? momentReportRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, pageSize))
            : momentReportRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest(page, pageSize));
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            toMomentReportResponses(result.getContent()),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    @Override
    @Transactional
    public MomentReportResponse reviewMomentReport(Long userId, Long reportId, ReviewMomentReportRequest request) {
        requireModerator(userId);
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("report status is required");
        }
        MomentReport report = momentReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("report not found"));
        report.setStatus(request.getStatus());
        report.setHandledBy(userId);
        report.setHandleNote(request.getHandleNote() == null ? "" : request.getHandleNote().trim());
        report.setHandledAt(LocalDateTime.now());
        momentReportRepository.save(report);
        Moment moment = momentRepository.findById(report.getMomentId()).orElse(null);
        if (Boolean.TRUE.equals(request.getDeleteMoment()) && moment != null && moment.getDeletedAt() == null) {
            moment.setDeletedAt(LocalDateTime.now());
            moment.setUpdatedAt(LocalDateTime.now());
            momentRepository.save(moment);
            saveNotification(moment.getAuthorId(), "moment_report_removed", "Moment removed", "Your moment was removed after review", moment.getId());
        }
        return toMomentReportResponses(List.of(report)).get(0);
    }

    @Override
    public PageResponse<NotificationResponse> listNotifications(Long userId, int page, int pageSize) {
        requireUser(userId);
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest(page, pageSize));
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            result.getContent().stream().map(NotificationResponse::new).toList(),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    @Override
    public long countUnreadNotifications(Long userId) {
        requireUser(userId);
        return notificationRepository.countByUserIdAndRead(userId, 0);
    }

    @Override
    @Transactional
    public void markNotificationRead(Long userId, Long notificationId) {
        requireUser(userId);
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
        notification.setRead(1);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsRead(Long userId) {
        requireUser(userId);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000)).forEach(notification -> {
            notification.setRead(1);
            notificationRepository.save(notification);
        });
    }

    private Moment requireMoment(Long momentId) {
        return momentRepository.findById(momentId)
            .filter(moment -> moment.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("moment not found"));
    }

    private Moment requireOwnedMoment(Long userId, Long momentId) {
        Moment moment = requireMoment(momentId);
        if (!moment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
        return moment;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private void requireModerator(Long userId) {
        requireUser(userId);
        if (moderationAdminUserIds == null || !moderationAdminUserIds.contains(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
    }

    private void saveNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private List<Long> safeFileIds(List<Long> fileIds) {
        if (fileIds == null) {
            return List.of();
        }
        List<Long> distinct = new LinkedHashSet<>(fileIds).stream().toList();
        if (distinct.size() > MAX_IMAGE_COUNT + MAX_VIDEO_COUNT) {
            throw new IllegalArgumentException("moment media must not exceed 12");
        }
        return distinct;
    }

    private void validateFiles(Long userId, List<Long> fileIds) {
        List<FileAsset> files = fileAssetRepository.findAllById(fileIds);
        if (files.size() != fileIds.size()) {
            throw new IllegalArgumentException("file not found");
        }
        if (files.stream().anyMatch(file -> !file.getUploaderId().equals(userId))) {
            throw new IllegalArgumentException("permission denied");
        }
        long imageCount = files.stream().filter(file -> startsWith(file.getMimeType(), "image/")).count();
        long videoCount = files.stream().filter(file -> startsWith(file.getMimeType(), "video/")).count();
        if (imageCount > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("moment images must not exceed 9");
        }
        if (videoCount > MAX_VIDEO_COUNT) {
            throw new IllegalArgumentException("moment videos must not exceed 3");
        }
        if (imageCount + videoCount != files.size()) {
            throw new IllegalArgumentException("moment media only supports images and videos");
        }
    }

    private boolean startsWith(String value, String prefix) {
        return value != null && value.startsWith(prefix);
    }

    private void validateContentAudit(String content) {
        String normalized = content == null ? "" : content.toLowerCase();
        BLOCKED_CONTENT_WORDS.stream()
            .filter(normalized::contains)
            .findFirst()
            .ifPresent(word -> {
                throw new IllegalArgumentException("moment content failed automatic moderation");
            });
    }

    private List<String> normalizeTags(List<String> inputTags, String content) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (inputTags != null) {
            inputTags.forEach(tag -> addTag(tags, tag));
        }
        if (content != null) {
            for (String part : content.split("\\s+")) {
                if (part.startsWith("#") && part.length() > 1) {
                    addTag(tags, part.substring(1));
                }
            }
        }
        if (tags.size() > MAX_TAG_COUNT) {
            throw new IllegalArgumentException("moment tags must not exceed 10");
        }
        return tags.stream().toList();
    }

    private void addTag(LinkedHashSet<String> tags, String rawTag) {
        if (rawTag == null) {
            return;
        }
        String tag = rawTag.trim().replaceFirst("^#+", "");
        tag = tag.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}_-]", "");
        if (!tag.isBlank()) {
            tags.add(tag.length() > 32 ? tag.substring(0, 32) : tag);
        }
    }

    private String cleanShortText(String value) {
        return value == null ? null : value.trim();
    }

    private void validateVisibility(String visibility) {
        if (visibility != null && !VISIBILITIES.contains(visibility)) {
            throw new IllegalArgumentException("visibility invalid");
        }
    }

    private List<Long> safeVisibleUserIds(List<Long> visibleUserIds) {
        if (visibleUserIds == null) {
            return List.of();
        }
        return new LinkedHashSet<>(visibleUserIds).stream().toList();
    }

    private void validateSpecifiedVisibility(Long userId, String visibility, List<Long> visibleUserIds) {
        if (!"specified".equals(visibility)) {
            return;
        }
        if (visibleUserIds.isEmpty()) {
            throw new IllegalArgumentException("specified visible users are required");
        }
        visibleUserIds.forEach(visibleUserId -> {
            requireUser(visibleUserId);
            if (visibleUserId.equals(userId) || !isFriend(userId, visibleUserId)) {
                throw new IllegalArgumentException("specified visible users must be friends");
            }
        });
    }

    private void saveVisibleUsers(Long momentId, List<Long> visibleUserIds) {
        for (Long visibleUserId : visibleUserIds) {
            MomentVisibleUser visibleUser = new MomentVisibleUser();
            visibleUser.setMomentId(momentId);
            visibleUser.setUserId(visibleUserId);
            visibleUser.setCreatedAt(LocalDateTime.now());
            momentVisibleUserRepository.save(visibleUser);
        }
    }

    private void saveMedia(Long momentId, List<Long> fileIds) {
        int order = 1;
        for (Long fileId : fileIds) {
            MomentMedia media = new MomentMedia();
            media.setMomentId(momentId);
            media.setFileId(fileId);
            media.setSortOrder(order++);
            media.setCreatedAt(LocalDateTime.now());
            momentMediaRepository.save(media);
        }
    }

    private PageRequest pageRequest(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return PageRequest.of(safePage - 1, safePageSize);
    }

    private PageResponse<MomentResponse> toMomentPage(Long userId, Page<Moment> result, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            toMomentResponses(userId, result.getContent()),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    private void requireCanViewMoment(Long userId, Moment moment) {
        if (!canViewMoment(userId, moment)) {
            throw new IllegalArgumentException("moment not found");
        }
    }

    private boolean canViewMoment(Long userId, Moment moment) {
        if (moment.getAuthorId().equals(userId)) {
            return true;
        }
        String visibility = moment.getVisibility() == null ? "public" : moment.getVisibility();
        if ("public".equals(visibility)) {
            return true;
        }
        if ("friends".equals(visibility)) {
            return isFriend(userId, moment.getAuthorId());
        }
        if ("specified".equals(visibility)) {
            return momentVisibleUserRepository.existsByMomentIdAndUserId(moment.getId(), userId);
        }
        return false;
    }

    private boolean isFriend(Long userId, Long friendId) {
        Long userAId = Math.min(userId, friendId);
        Long userBId = Math.max(userId, friendId);
        return friendshipRepository.existsByUserAIdAndUserBIdAndDeletedAtIsNull(userAId, userBId);
    }

    private MomentResponse toMomentResponse(Long userId, Moment moment) {
        return toMomentResponses(userId, List.of(moment)).get(0);
    }

    private List<MomentResponse> toMomentResponses(Long userId, List<Moment> moments) {
        List<Long> momentIds = moments.stream().map(Moment::getId).toList();
        Map<Long, User> authors = userRepository.findAllById(moments.stream().map(Moment::getAuthorId).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        List<MomentMedia> media = momentIds.isEmpty()
            ? List.of()
            : momentMediaRepository.findByMomentIdInOrderBySortOrderAsc(momentIds);
        Map<Long, FileAsset> files = fileAssetRepository.findAllById(media.stream().map(MomentMedia::getFileId).toList())
            .stream()
            .collect(Collectors.toMap(FileAsset::getId, Function.identity()));
        Map<Long, List<MomentMediaResponse>> mediaMap = media.stream().collect(Collectors.groupingBy(
            MomentMedia::getMomentId,
            Collectors.mapping(item -> new MomentMediaResponse(item, files.get(item.getFileId())), Collectors.toList())
        ));
        Map<Long, List<Long>> visibleUserMap = momentVisibleUserRepository.findByMomentIdIn(momentIds).stream()
            .collect(Collectors.groupingBy(
                MomentVisibleUser::getMomentId,
                Collectors.mapping(MomentVisibleUser::getUserId, Collectors.toList())
            ));
        return moments.stream().map(moment -> new MomentResponse(
            moment,
            authors.get(moment.getAuthorId()),
            mediaMap.getOrDefault(moment.getId(), List.of()),
            moment.getAuthorId().equals(userId) ? visibleUserMap.getOrDefault(moment.getId(), List.of()) : List.of(),
            momentLikeRepository.findByMomentIdAndUserId(moment.getId(), userId).isPresent(),
            momentCollectRepository.findByMomentIdAndUserId(moment.getId(), userId).isPresent()
        )).toList();
    }

    private List<MomentReportResponse> toMomentReportResponses(List<MomentReport> reports) {
        Map<Long, Moment> moments = momentRepository.findAllById(reports.stream().map(MomentReport::getMomentId).toList())
            .stream()
            .collect(Collectors.toMap(Moment::getId, Function.identity()));
        Set<Long> userIds = new LinkedHashSet<>();
        reports.forEach(report -> {
            userIds.add(report.getReporterId());
            Moment moment = moments.get(report.getMomentId());
            if (moment != null) {
                userIds.add(moment.getAuthorId());
            }
        });
        Map<Long, User> users = userRepository.findAllById(userIds)
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return reports.stream().map(report -> {
            Moment moment = moments.get(report.getMomentId());
            User author = moment == null ? null : users.get(moment.getAuthorId());
            return new MomentReportResponse(report, moment, users.get(report.getReporterId()), author);
        }).toList();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}

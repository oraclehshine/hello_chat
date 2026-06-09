package com.hellochat.backend.service.impl;

import com.hellochat.backend.cache.PresenceCacheService;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.dto.PresenceResponse;
import com.hellochat.backend.dto.RecommendationResponse;
import com.hellochat.backend.dto.SearchHistoryResponse;
import com.hellochat.backend.dto.SocialSearchResponse;
import com.hellochat.backend.dto.TopicRecommendationResponse;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.entity.ChatGroup;
import com.hellochat.backend.entity.GroupMember;
import com.hellochat.backend.entity.Moment;
import com.hellochat.backend.entity.SearchHistory;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.entity.UserPresence;
import com.hellochat.backend.repository.ChatGroupRepository;
import com.hellochat.backend.repository.FriendshipRepository;
import com.hellochat.backend.repository.GroupMemberRepository;
import com.hellochat.backend.repository.MomentRepository;
import com.hellochat.backend.repository.SearchHistoryRepository;
import com.hellochat.backend.repository.UserBlockRepository;
import com.hellochat.backend.repository.UserPresenceRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.AdminDashboardAsyncService;
import com.hellochat.backend.service.SocialService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialServiceImpl implements SocialService {

    private static final Set<String> PRESENCE_STATUSES = Set.of("online", "offline", "busy", "invisible");

    private final UserRepository userRepository;
    private final UserPresenceRepository userPresenceRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final MomentRepository momentRepository;
    private final AdminDashboardAsyncService adminDashboardAsyncService;
    private final PresenceCacheService presenceCacheService;

    public SocialServiceImpl(
        UserRepository userRepository,
        UserPresenceRepository userPresenceRepository,
        SearchHistoryRepository searchHistoryRepository,
        ChatGroupRepository chatGroupRepository,
        GroupMemberRepository groupMemberRepository,
        FriendshipRepository friendshipRepository,
        UserBlockRepository userBlockRepository,
        MomentRepository momentRepository,
        AdminDashboardAsyncService adminDashboardAsyncService,
        PresenceCacheService presenceCacheService
    ) {
        this.userRepository = userRepository;
        this.userPresenceRepository = userPresenceRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.chatGroupRepository = chatGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBlockRepository = userBlockRepository;
        this.momentRepository = momentRepository;
        this.adminDashboardAsyncService = adminDashboardAsyncService;
        this.presenceCacheService = presenceCacheService;
    }

    @Override
    @Transactional
    public PresenceResponse updatePresence(Long userId, String status) {
        requireUser(userId);
        String safeStatus = status == null ? "online" : status.trim().toLowerCase();
        if (!PRESENCE_STATUSES.contains(safeStatus)) {
            throw new IllegalArgumentException("presence status invalid");
        }
        UserPresence presence = userPresenceRepository.findById(userId).orElseGet(UserPresence::new);
        presence.setUserId(userId);
        presence.setStatus(safeStatus);
        if (!"invisible".equals(safeStatus)) {
            presence.setLastActiveAt(LocalDateTime.now());
        }
        presence.setUpdatedAt(LocalDateTime.now());
        UserPresence saved = userPresenceRepository.save(presence);
        presenceCacheService.put(userId, saved.getStatus(), saved.getLastActiveAt(), saved.getUpdatedAt());
        adminDashboardAsyncService.recordPresence(userId, safeStatus);
        return new PresenceResponse(saved);
    }

    @Override
    public PresenceResponse getPresence(Long userId) {
        requireUser(userId);
        var cachedPresence = presenceCacheService.get(userId);
        if (cachedPresence.isPresent()) {
            return new PresenceResponse(cachedPresence.get());
        }
        return new PresenceResponse(userPresenceRepository.findById(userId).orElseGet(() -> {
            UserPresence presence = new UserPresence();
            presence.setUserId(userId);
            presence.setStatus("offline");
            return presence;
        }));
    }

    @Override
    @Transactional
    public SocialSearchResponse search(Long userId, String keyword) {
        requireUser(userId);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        if (!safeKeyword.isBlank()) {
            saveHistory(userId, safeKeyword, "mixed");
        }
        var users = userRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            safeKeyword,
            safeKeyword,
            PageRequest.of(0, 10)
        ).getContent().stream()
            .filter(user -> !user.getId().equals(userId))
            .map(UserProfileResponse::new)
            .toList();
        var groups = chatGroupRepository.findByNameContainingIgnoreCaseAndStatus(safeKeyword, 1, PageRequest.of(0, 10))
            .getContent().stream()
            .map(group -> new GroupResponse(group, groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(group.getId(), 1)))
            .toList();
        return new SocialSearchResponse(users, groups);
    }

    @Override
    public List<SearchHistoryResponse> listSearchHistory(Long userId) {
        requireUser(userId);
        return searchHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(SearchHistoryResponse::new)
            .toList();
    }

    @Override
    public RecommendationResponse recommend(Long userId) {
        requireUser(userId);
        return new RecommendationResponse(
            recommendFriends(userId),
            recommendGroups(userId),
            recommendTopics()
        );
    }

    private List<UserProfileResponse> recommendFriends(Long userId) {
        return userRepository.findAll(PageRequest.of(0, 50)).getContent().stream()
            .filter(user -> !user.getId().equals(userId))
            .filter(user -> !isFriend(userId, user.getId()))
            .filter(user -> !isBlockedEitherWay(userId, user.getId()))
            .limit(8)
            .map(UserProfileResponse::new)
            .toList();
    }

    private List<GroupResponse> recommendGroups(Long userId) {
        return chatGroupRepository.findByStatusOrderByCreatedAtDesc(1, PageRequest.of(0, 30)).getContent().stream()
            .filter(group -> !groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNullAndStatus(group.getId(), userId, 1))
            .limit(6)
            .map(group -> new GroupResponse(group, groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(group.getId(), 1)))
            .toList();
    }

    private List<TopicRecommendationResponse> recommendTopics() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Moment moment : momentRepository.findTop100ByDeletedAtIsNullAndTagsIsNotNullOrderByCreatedAtDesc()) {
            Arrays.stream(moment.getTags().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .forEach(tag -> counts.merge(tag, 1L, Long::sum));
        }
        return counts.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(8)
            .map(entry -> new TopicRecommendationResponse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private void saveHistory(Long userId, String keyword, String searchType) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword.length() > 128 ? keyword.substring(0, 128) : keyword);
        history.setSearchType(searchType);
        history.setCreatedAt(LocalDateTime.now());
        searchHistoryRepository.save(history);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private boolean isFriend(Long userId, Long targetUserId) {
        long userAId = Math.min(userId, targetUserId);
        long userBId = Math.max(userId, targetUserId);
        return friendshipRepository.existsByUserAIdAndUserBIdAndDeletedAtIsNull(userAId, userBId);
    }

    private boolean isBlockedEitherWay(Long userId, Long targetUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId)
            || userBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, userId);
    }
}

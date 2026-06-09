package com.hellochat.backend.service.impl;

import com.hellochat.backend.dto.CreateGroupRequest;
import com.hellochat.backend.dto.GroupJoinRequestRequest;
import com.hellochat.backend.dto.GroupJoinRequestResponse;
import com.hellochat.backend.dto.GroupMemberResponse;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupNoticeReadResponse;
import com.hellochat.backend.dto.GroupNotificationResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.UpdateGroupNoticeRequest;
import com.hellochat.backend.dto.UpdateGroupRequest;
import com.hellochat.backend.dto.UpdateGroupNicknameRequest;
import com.hellochat.backend.entity.ChatGroup;
import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.GroupJoinRequest;
import com.hellochat.backend.entity.GroupMember;
import com.hellochat.backend.entity.GroupMessage;
import com.hellochat.backend.entity.GroupMessageMention;
import com.hellochat.backend.entity.GroupNotification;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.repository.ChatGroupRepository;
import com.hellochat.backend.repository.FileAssetRepository;
import com.hellochat.backend.repository.FriendshipRepository;
import com.hellochat.backend.repository.GroupJoinRequestRepository;
import com.hellochat.backend.repository.GroupMemberRepository;
import com.hellochat.backend.repository.GroupMessageRepository;
import com.hellochat.backend.repository.GroupMessageMentionRepository;
import com.hellochat.backend.repository.GroupNotificationRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.AdminDashboardAsyncService;
import com.hellochat.backend.service.GroupPushService;
import com.hellochat.backend.service.GroupService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupServiceImpl implements GroupService {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of("text", "image", "file");

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final FriendshipRepository friendshipRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupMessageMentionRepository groupMessageMentionRepository;
    private final GroupNotificationRepository groupNotificationRepository;
    private final GroupPushService groupPushService;
    private final AdminDashboardAsyncService adminDashboardAsyncService;

    public GroupServiceImpl(
        ChatGroupRepository chatGroupRepository,
        GroupMemberRepository groupMemberRepository,
        GroupMessageRepository groupMessageRepository,
        UserRepository userRepository,
        FileAssetRepository fileAssetRepository,
        FriendshipRepository friendshipRepository,
        GroupJoinRequestRepository groupJoinRequestRepository,
        GroupMessageMentionRepository groupMessageMentionRepository,
        GroupNotificationRepository groupNotificationRepository,
        GroupPushService groupPushService,
        AdminDashboardAsyncService adminDashboardAsyncService
    ) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.userRepository = userRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.friendshipRepository = friendshipRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.groupMessageMentionRepository = groupMessageMentionRepository;
        this.groupNotificationRepository = groupNotificationRepository;
        this.groupPushService = groupPushService;
        this.adminDashboardAsyncService = adminDashboardAsyncService;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        requireUser(userId);
        Set<Long> memberIds = new LinkedHashSet<>();
        memberIds.add(userId);
        if (request.getMemberIds() != null) {
            memberIds.addAll(request.getMemberIds());
        }
        if (memberIds.size() < 2) {
            throw new IllegalArgumentException("group requires at least 2 members");
        }
        if (memberIds.size() > 100) {
            throw new IllegalArgumentException("initial members must not exceed 100");
        }
        memberIds.stream()
            .filter(memberId -> !memberId.equals(userId))
            .forEach(memberId -> requireFriend(userId, memberId));
        List<User> users = userRepository.findAllById(memberIds);
        if (users.size() != memberIds.size()) {
            throw new IllegalArgumentException("member not found");
        }

        ChatGroup group = new ChatGroup();
        group.setOwnerId(userId);
        group.setName(request.getGroupName().trim());
        group.setDescription(request.getDescription());
        group.setAvatarUrl(request.getAvatarUrl());
        group.setInviteCode(generateInviteCode());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        ChatGroup savedGroup = chatGroupRepository.save(group);

        for (Long memberId : memberIds) {
            GroupMember member = new GroupMember();
            member.setGroupId(savedGroup.getId());
            member.setUserId(memberId);
            member.setRole(memberId.equals(userId) ? GroupMember.ROLE_OWNER : GroupMember.ROLE_MEMBER);
            member.setJoinedAt(LocalDateTime.now());
            member.setStatus(GroupMember.STATUS_ACTIVE);
            groupMemberRepository.save(member);
        }
        GroupResponse response = toGroupResponse(savedGroup);
        saveNotification(savedGroup.getId(), userId, null, "group:created", "Group created");
        groupPushService.pushGroupUpdated(savedGroup.getId(), "group:created", response);
        adminDashboardAsyncService.recordGroupCreated(savedGroup.getId(), userId, savedGroup.getName());
        return response;
    }

    @Override
    public List<GroupResponse> listMyGroups(Long userId) {
        requireUser(userId);
        return groupMemberRepository.findByUserIdAndLeftAtIsNullAndStatusOrderByJoinedAtDesc(userId, GroupMember.STATUS_ACTIVE)
            .stream()
            .map(member -> chatGroupRepository.findById(member.getGroupId())
                .map(group -> toGroupResponse(group, member))
                .orElse(null))
            .filter(response -> response != null)
            .toList();
    }

    @Override
    public GroupResponse getGroup(Long userId, Long groupId) {
        requireActiveMember(userId, groupId);
        return toGroupResponse(requireGroup(groupId));
    }

    @Override
    public PageResponse<GroupResponse> searchGroups(Long userId, String keyword, int page, int pageSize) {
        requireUser(userId);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<ChatGroup> result = chatGroupRepository
            .findByNameContainingIgnoreCaseAndStatus(safeKeyword, 1, pageRequest(page, pageSize));
        return new PageResponse<>(
            result.getContent().stream().map(this::toGroupResponse).toList(),
            Math.max(page, 1),
            Math.min(Math.max(pageSize, 1), 100),
            result.getTotalElements()
        );
    }

    @Override
    @Transactional
    public GroupResponse joinByInviteCode(Long userId, String inviteCode) {
        requireUser(userId);
        ChatGroup group = chatGroupRepository.findByInviteCodeAndStatus(inviteCode, 1)
            .orElseThrow(() -> new IllegalArgumentException("invite code invalid"));
        if (!isActiveMember(userId, group.getId())) {
            addMemberEntity(group, userId);
            group.setUpdatedAt(LocalDateTime.now());
            chatGroupRepository.save(group);
            saveNotification(group.getId(), userId, userId, "group:member_joined", "Member joined by invite link");
            groupPushService.pushGroupUpdated(group.getId(), "group:member_joined", toGroupResponse(group));
        }
        return toGroupResponse(group);
    }

    @Override
    @Transactional
    public GroupJoinRequestResponse requestJoinGroup(Long userId, Long groupId, GroupJoinRequestRequest request) {
        requireUser(userId);
        ChatGroup group = requireGroup(groupId);
        if (group.getStatus() == null || group.getStatus() != 1) {
            throw new IllegalArgumentException("group not available");
        }
        if (isActiveMember(userId, groupId)) {
            throw new IllegalArgumentException("already in group");
        }
        groupJoinRequestRepository.findFirstByGroupIdAndRequesterIdAndStatusOrderByCreatedAtDesc(
            groupId,
            userId,
            GroupJoinRequest.STATUS_PENDING
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("join request already pending");
        });
        GroupJoinRequest joinRequest = new GroupJoinRequest();
        joinRequest.setGroupId(groupId);
        joinRequest.setRequesterId(userId);
        joinRequest.setMessage(request.getMessage() == null ? "" : request.getMessage().trim());
        joinRequest.setCreatedAt(LocalDateTime.now());
        joinRequest.setUpdatedAt(LocalDateTime.now());
        GroupJoinRequest saved = groupJoinRequestRepository.save(joinRequest);
        saveNotification(groupId, userId, null, "group:join_requested", "Join request submitted");
        groupPushService.pushGroupUpdated(groupId, "group:join_requested", toGroupResponse(group));
        return new GroupJoinRequestResponse(saved, requireUser(userId));
    }

    @Override
    public List<GroupJoinRequestResponse> listJoinRequests(Long userId, Long groupId) {
        requireManager(userId, groupId);
        List<GroupJoinRequest> requests = groupJoinRequestRepository
            .findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, GroupJoinRequest.STATUS_PENDING);
        Map<Long, User> users = userRepository.findAllById(requests.stream().map(GroupJoinRequest::getRequesterId).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return requests.stream()
            .map(request -> new GroupJoinRequestResponse(request, users.get(request.getRequesterId())))
            .toList();
    }

    @Override
    public List<GroupJoinRequestResponse> listMyJoinRequests(Long userId) {
        requireUser(userId);
        List<GroupJoinRequest> requests = groupJoinRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId);
        User requester = requireUser(userId);
        return requests.stream()
            .map(request -> new GroupJoinRequestResponse(request, requester))
            .toList();
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> reviewJoinRequest(Long userId, Long groupId, Long requestId, boolean approve) {
        requireManager(userId, groupId);
        GroupJoinRequest request = groupJoinRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("join request not found"));
        if (!request.getGroupId().equals(groupId) || request.getStatus() != GroupJoinRequest.STATUS_PENDING) {
            throw new IllegalArgumentException("join request not found");
        }
        request.setStatus(approve ? GroupJoinRequest.STATUS_APPROVED : GroupJoinRequest.STATUS_REJECTED);
        request.setHandledBy(userId);
        request.setHandledAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        groupJoinRequestRepository.save(request);
        ChatGroup group = requireGroup(groupId);
        if (approve && !isActiveMember(request.getRequesterId(), groupId)) {
            addMemberEntity(group, request.getRequesterId());
            group.setUpdatedAt(LocalDateTime.now());
            chatGroupRepository.save(group);
        }
        saveNotification(
            groupId,
            userId,
            request.getRequesterId(),
            approve ? "group:join_approved" : "group:join_rejected",
            approve ? "Join request approved" : "Join request rejected"
        );
        groupPushService.pushGroupUpdated(groupId, approve ? "group:join_approved" : "group:join_rejected", toGroupResponse(group));
        return listMembers(userId, groupId);
    }

    @Override
    public PageResponse<GroupNotificationResponse> listNotifications(Long userId, Long groupId, int page, int pageSize) {
        requireActiveMember(userId, groupId);
        Page<GroupNotification> result = groupNotificationRepository.findByGroupIdOrderByCreatedAtDesc(
            groupId,
            pageRequest(page, pageSize)
        );
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            result.getContent().stream().map(GroupNotificationResponse::new).toList(),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    @Override
    public List<GroupMemberResponse> listMembers(Long userId, Long groupId) {
        requireActiveMember(userId, groupId);
        List<GroupMember> members = groupMemberRepository
            .findByGroupIdAndLeftAtIsNullAndStatusOrderByRoleDescJoinedAtAsc(groupId, GroupMember.STATUS_ACTIVE);
        Map<Long, User> users = userRepository.findAllById(members.stream().map(GroupMember::getUserId).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
            .map(member -> new GroupMemberResponse(member, users.get(member.getUserId())))
            .toList();
    }

    @Override
    public PageResponse<GroupMessageResponse> listMessages(Long userId, Long groupId, int page, int pageSize) {
        requireActiveMember(userId, groupId);
        Page<GroupMessage> result = groupMessageRepository
            .findByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(groupId, pageRequest(page, pageSize));
        return toMessagePage(result, page, pageSize);
    }

    @Override
    @Transactional
    public void markGroupAsRead(Long userId, Long groupId) {
        GroupMember member = requireActiveMember(userId, groupId);
        member.setLastReadAt(LocalDateTime.now());
        groupMemberRepository.save(member);
    }

    @Override
    public PageResponse<GroupMessageResponse> searchMessages(Long userId, Long groupId, String keyword, int page, int pageSize) {
        requireActiveMember(userId, groupId);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<GroupMessage> result = groupMessageRepository
            .findByGroupIdAndContentContainingIgnoreCaseAndDeletedAtIsNullOrderBySentAtDesc(
                groupId,
                safeKeyword,
                pageRequest(page, pageSize)
        );
        return toMessagePage(result, page, pageSize);
    }

    @Override
    public PageResponse<GroupMessageResponse> listFiles(Long userId, Long groupId, int page, int pageSize) {
        requireActiveMember(userId, groupId);
        Page<GroupMessage> result = groupMessageRepository
            .findByGroupIdAndMessageTypeInAndFileIdIsNotNullAndDeletedAtIsNullOrderBySentAtDesc(
                groupId,
                List.of("image", "file"),
                pageRequest(page, pageSize)
            );
        return toMessagePage(result, page, pageSize);
    }

    @Override
    @Transactional
    public GroupMessageResponse sendMessage(Long userId, Long groupId, SendMessageRequest request) {
        GroupMember senderMember = requireActiveMember(userId, groupId);
        ChatGroup group = requireGroup(groupId);
        if (group.getChatEnabled() != null && group.getChatEnabled() == 0 && senderMember.getRole() < GroupMember.ROLE_ADMIN) {
            throw new IllegalArgumentException("group chat is closed");
        }
        if (senderMember.getMuteUntil() != null && senderMember.getMuteUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("member is muted");
        }
        if (!SUPPORTED_MESSAGE_TYPES.contains(request.getMessageType())) {
            throw new IllegalArgumentException("message type unsupported");
        }
        if ("text".equals(request.getMessageType()) && (request.getContent() == null || request.getContent().isBlank())) {
            throw new IllegalArgumentException("message content is required");
        }
        FileAsset fileAsset = null;
        if ("image".equals(request.getMessageType()) || "file".equals(request.getMessageType())) {
            if (request.getFileId() == null) {
                throw new IllegalArgumentException("fileId is required");
            }
            fileAsset = fileAssetRepository.findById(request.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("file not found"));
            if (!fileAsset.getUploaderId().equals(userId)) {
                throw new IllegalArgumentException("permission denied");
            }
            request.setContent(fileAsset.getFileUrl());
        }
        if (request.getReplyToMessageId() != null) {
            GroupMessage replyMessage = groupMessageRepository.findById(request.getReplyToMessageId())
                .orElseThrow(() -> new IllegalArgumentException("reply message not found"));
            if (!replyMessage.getGroupId().equals(groupId)) {
                throw new IllegalArgumentException("reply message not found");
            }
        }
        Set<Long> mentionUserIds = request.getMentionUserIds() == null
            ? Set.of()
            : new LinkedHashSet<>(request.getMentionUserIds());
        for (Long mentionUserId : mentionUserIds) {
            requireActiveMember(mentionUserId, groupId);
        }
        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(userId);
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setFileId(request.getFileId());
        message.setReplyToMessageId(request.getReplyToMessageId());
        message.setMentionAll(Boolean.TRUE.equals(request.getMentionAll()) ? 1 : 0);
        message.setSentAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        GroupMessage saved = groupMessageRepository.save(message);
        for (Long mentionUserId : mentionUserIds) {
            GroupMessageMention mention = new GroupMessageMention();
            mention.setMessageId(saved.getId());
            mention.setUserId(mentionUserId);
            groupMessageMentionRepository.save(mention);
        }
        GroupMessage replyMessage = saved.getReplyToMessageId() == null
            ? null
            : groupMessageRepository.findById(saved.getReplyToMessageId()).orElse(null);
        GroupMessageResponse response = new GroupMessageResponse(saved, requireUser(userId), fileAsset, replyMessage, List.copyOf(mentionUserIds));
        groupPushService.pushNewMessage(groupId, response);
        adminDashboardAsyncService.recordGroupMessage(groupId, userId, request.getMessageType(), request.getContent());
        return response;
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long groupId, Long messageId) {
        GroupMessage message = requireMessageSender(userId, groupId, messageId);
        int recallLimit = requireGroup(groupId).getRecallLimitMinutes() == null ? 2 : requireGroup(groupId).getRecallLimitMinutes();
        if (recallLimit >= 0 && message.getSentAt().isBefore(LocalDateTime.now().minusMinutes(recallLimit))) {
            throw new IllegalArgumentException("message recall window expired");
        }
        message.setRecallStatus(GroupMessage.RECALL_RECALLED);
        message.setContent("");
        message.setUpdatedAt(LocalDateTime.now());
        groupMessageRepository.save(message);
        groupPushService.pushMessageUpdated(message.getGroupId(), messageId, "group:message_recalled");
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long groupId, Long messageId) {
        GroupMessage message = groupMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        if (!message.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("message not found");
        }
        requireActiveMember(userId, message.getGroupId());
        message.setDeletedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        groupMessageRepository.save(message);
        groupPushService.pushMessageUpdated(message.getGroupId(), messageId, "group:message_deleted");
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> addMembers(Long userId, Long groupId, List<Long> memberIds) {
        requireManager(userId, groupId);
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("memberIds is required");
        }
        Set<Long> distinctMemberIds = new LinkedHashSet<>(memberIds);
        List<User> users = userRepository.findAllById(distinctMemberIds);
        if (users.size() != distinctMemberIds.size()) {
            throw new IllegalArgumentException("member not found");
        }
        distinctMemberIds.forEach(memberId -> requireFriend(userId, memberId));
        ChatGroup group = requireGroup(groupId);
        long currentCount = groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(groupId, GroupMember.STATUS_ACTIVE);
        if (currentCount + distinctMemberIds.size() > group.getMaxMemberCount()) {
            throw new IllegalArgumentException("group member limit exceeded");
        }
        for (Long memberId : distinctMemberIds) {
            if (groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNullAndStatus(
                groupId,
                memberId,
                GroupMember.STATUS_ACTIVE
            )) {
                continue;
            }
            addMemberEntity(group, memberId);
        }
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        saveNotification(groupId, userId, null, "group:members_added", "Members added");
        GroupResponse response = toGroupResponse(group);
        groupPushService.pushGroupUpdated(groupId, "group:members_added", response);
        return listMembers(userId, groupId);
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long groupId, Long memberUserId) {
        GroupMember manager = requireManager(userId, groupId);
        GroupMember member = requireActiveMember(memberUserId, groupId);
        if (member.getRole() == GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("owner cannot be removed");
        }
        if (manager.getRole() == GroupMember.ROLE_ADMIN && member.getRole() >= GroupMember.ROLE_ADMIN) {
            throw new IllegalArgumentException("permission denied");
        }
        member.setLeftAt(LocalDateTime.now());
        member.setStatus(0);
        groupMemberRepository.save(member);
        ChatGroup group = requireGroup(groupId);
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        saveNotification(groupId, userId, memberUserId, "group:member_removed", "Member removed");
        groupPushService.pushGroupUpdated(groupId, "group:member_removed", toGroupResponse(group));
    }

    @Override
    @Transactional
    public GroupResponse updateNotice(Long userId, Long groupId, UpdateGroupNoticeRequest request) {
        requireManager(userId, groupId);
        ChatGroup group = requireGroup(groupId);
        group.setNotice(request.getNotice() == null ? "" : request.getNotice().trim());
        group.setNoticeUpdatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        GroupResponse response = toGroupResponse(chatGroupRepository.save(group));
        saveNotification(groupId, userId, null, "group:notice_updated", "Group announcement updated");
        groupPushService.pushGroupUpdated(groupId, "group:notice_updated", response);
        return response;
    }

    @Override
    @Transactional
    public GroupNoticeReadResponse markNoticeRead(Long userId, Long groupId) {
        GroupMember member = requireActiveMember(userId, groupId);
        member.setNoticeReadAt(LocalDateTime.now());
        groupMemberRepository.save(member);
        return getNoticeReadStats(userId, groupId);
    }

    @Override
    public GroupNoticeReadResponse getNoticeReadStats(Long userId, Long groupId) {
        requireActiveMember(userId, groupId);
        ChatGroup group = requireGroup(groupId);
        long memberCount = groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(groupId, GroupMember.STATUS_ACTIVE);
        long readCount = group.getNoticeUpdatedAt() == null
            ? memberCount
            : groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatusAndNoticeReadAtGreaterThanEqual(
                groupId,
                GroupMember.STATUS_ACTIVE,
                group.getNoticeUpdatedAt()
            );
        return new GroupNoticeReadResponse(readCount, memberCount);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long userId, Long groupId, UpdateGroupRequest request) {
        requireManager(userId, groupId);
        ChatGroup group = requireGroup(groupId);
        if (request.getGroupName() != null) {
            group.setName(request.getGroupName().trim());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().trim());
        }
        if (request.getAvatarUrl() != null) {
            group.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getChatEnabled() != null) {
            group.setChatEnabled(Boolean.TRUE.equals(request.getChatEnabled()) ? 1 : 0);
        }
        if (request.getRecallLimitMinutes() != null) {
            if (request.getRecallLimitMinutes() < 0 || request.getRecallLimitMinutes() > 1440) {
                throw new IllegalArgumentException("recall limit invalid");
            }
            group.setRecallLimitMinutes(request.getRecallLimitMinutes());
        }
        group.setUpdatedAt(LocalDateTime.now());
        GroupResponse response = toGroupResponse(chatGroupRepository.save(group));
        saveNotification(groupId, userId, null, "group:profile_updated", "Group profile updated");
        groupPushService.pushGroupUpdated(groupId, "group:profile_updated", response);
        return response;
    }

    @Override
    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = requireActiveMember(userId, groupId);
        if (member.getRole() == GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("owner must transfer or dissolve group first");
        }
        member.setLeftAt(LocalDateTime.now());
        member.setStatus(0);
        groupMemberRepository.save(member);
        ChatGroup group = requireGroup(groupId);
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        saveNotification(groupId, userId, userId, "group:member_left", "Member left");
        groupPushService.pushGroupUpdated(groupId, "group:member_left", toGroupResponse(group));
    }

    @Override
    @Transactional
    public void dissolveGroup(Long userId, Long groupId) {
        GroupMember owner = requireActiveMember(userId, groupId);
        if (owner.getRole() == null || owner.getRole() != GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("permission denied");
        }
        ChatGroup group = requireGroup(groupId);
        group.setStatus(0);
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        groupMemberRepository.findByGroupIdAndLeftAtIsNullAndStatusOrderByRoleDescJoinedAtAsc(groupId, GroupMember.STATUS_ACTIVE)
            .forEach(member -> {
                member.setLeftAt(LocalDateTime.now());
                member.setStatus(0);
                groupMemberRepository.save(member);
            });
        saveNotification(groupId, userId, null, "group:dissolved", "Group dissolved");
        groupPushService.pushGroupUpdated(groupId, "group:dissolved", toGroupResponse(group));
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> transferOwner(Long userId, Long groupId, Long targetUserId) {
        GroupMember owner = requireActiveMember(userId, groupId);
        if (owner.getRole() == null || owner.getRole() != GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("permission denied");
        }
        GroupMember target = requireActiveMember(targetUserId, groupId);
        owner.setRole(GroupMember.ROLE_MEMBER);
        target.setRole(GroupMember.ROLE_OWNER);
        groupMemberRepository.save(owner);
        groupMemberRepository.save(target);
        ChatGroup group = requireGroup(groupId);
        group.setOwnerId(targetUserId);
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        saveNotification(groupId, userId, targetUserId, "group:owner_transferred", "Group owner transferred");
        groupPushService.pushGroupUpdated(groupId, "group:owner_transferred", toGroupResponse(group));
        return listMembers(targetUserId, groupId);
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> updateMyNickname(Long userId, Long groupId, UpdateGroupNicknameRequest request) {
        GroupMember member = requireActiveMember(userId, groupId);
        String nickname = request.getNickname() == null ? null : request.getNickname().trim();
        member.setNickname(nickname == null || nickname.isBlank() ? null : nickname);
        groupMemberRepository.save(member);
        groupPushService.pushGroupUpdated(groupId, "group:member_nickname_updated", toGroupResponse(requireGroup(groupId)));
        return listMembers(userId, groupId);
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> setAdmin(Long userId, Long groupId, Long memberUserId, boolean admin) {
        GroupMember owner = requireActiveMember(userId, groupId);
        if (owner.getRole() == null || owner.getRole() != GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("permission denied");
        }
        GroupMember member = requireActiveMember(memberUserId, groupId);
        if (member.getRole() == GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("owner role cannot be changed");
        }
        member.setRole(admin ? GroupMember.ROLE_ADMIN : GroupMember.ROLE_MEMBER);
        groupMemberRepository.save(member);
        ChatGroup group = requireGroup(groupId);
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupRepository.save(group);
        saveNotification(groupId, userId, memberUserId, admin ? "group:admin_set" : "group:admin_unset", admin ? "Admin set" : "Admin unset");
        groupPushService.pushGroupUpdated(groupId, admin ? "group:admin_set" : "group:admin_unset", toGroupResponse(group));
        return listMembers(userId, groupId);
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> muteMember(Long userId, Long groupId, Long memberUserId, int minutes) {
        GroupMember manager = requireManager(userId, groupId);
        GroupMember member = requireActiveMember(memberUserId, groupId);
        ensureCanManageMember(manager, member);
        member.setMuteUntil(LocalDateTime.now().plusMinutes(minutes));
        groupMemberRepository.save(member);
        saveNotification(groupId, userId, memberUserId, "group:member_muted", "Member muted");
        groupPushService.pushGroupUpdated(groupId, "group:member_muted", toGroupResponse(requireGroup(groupId)));
        return listMembers(userId, groupId);
    }

    @Override
    @Transactional
    public List<GroupMemberResponse> unmuteMember(Long userId, Long groupId, Long memberUserId) {
        GroupMember manager = requireManager(userId, groupId);
        GroupMember member = requireActiveMember(memberUserId, groupId);
        ensureCanManageMember(manager, member);
        member.setMuteUntil(null);
        groupMemberRepository.save(member);
        saveNotification(groupId, userId, memberUserId, "group:member_unmuted", "Member unmuted");
        groupPushService.pushGroupUpdated(groupId, "group:member_unmuted", toGroupResponse(requireGroup(groupId)));
        return listMembers(userId, groupId);
    }

    private ChatGroup requireGroup(Long groupId) {
        return chatGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("group not found"));
    }

    private GroupMember requireActiveMember(Long userId, Long groupId) {
        requireGroup(groupId);
        return groupMemberRepository.findByGroupIdAndUserIdAndLeftAtIsNullAndStatus(
            groupId,
            userId,
            GroupMember.STATUS_ACTIVE
        ).orElseThrow(() -> new IllegalArgumentException("permission denied"));
    }

    private GroupMember requireManager(Long userId, Long groupId) {
        GroupMember member = requireActiveMember(userId, groupId);
        if (member.getRole() == null || member.getRole() < GroupMember.ROLE_ADMIN) {
            throw new IllegalArgumentException("permission denied");
        }
        return member;
    }

    private boolean isActiveMember(Long userId, Long groupId) {
        return groupMemberRepository.existsByGroupIdAndUserIdAndLeftAtIsNullAndStatus(
            groupId,
            userId,
            GroupMember.STATUS_ACTIVE
        );
    }

    private void addMemberEntity(ChatGroup group, Long memberId) {
        long currentCount = groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(group.getId(), GroupMember.STATUS_ACTIVE);
        if (currentCount + 1 > group.getMaxMemberCount()) {
            throw new IllegalArgumentException("group member limit exceeded");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(memberId);
        member.setRole(GroupMember.ROLE_MEMBER);
        member.setJoinedAt(LocalDateTime.now());
        member.setStatus(GroupMember.STATUS_ACTIVE);
        groupMemberRepository.save(member);
    }

    private void ensureCanManageMember(GroupMember manager, GroupMember member) {
        if (member.getRole() == GroupMember.ROLE_OWNER) {
            throw new IllegalArgumentException("owner cannot be managed");
        }
        if (manager.getRole() == GroupMember.ROLE_ADMIN && member.getRole() >= GroupMember.ROLE_ADMIN) {
            throw new IllegalArgumentException("permission denied");
        }
    }

    private GroupMessage requireMessageSender(Long userId, Long groupId, Long messageId) {
        GroupMessage message = groupMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        if (!message.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("message not found");
        }
        requireActiveMember(userId, message.getGroupId());
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
        return message;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private void requireFriend(Long userId, Long targetUserId) {
        friendshipRepository.findByUserAIdAndUserBId(Math.min(userId, targetUserId), Math.max(userId, targetUserId))
            .filter(friendship -> friendship.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("only friends can be invited"));
    }

    private void saveNotification(Long groupId, Long actorId, Long targetUserId, String noticeType, String content) {
        GroupNotification notification = new GroupNotification();
        notification.setGroupId(groupId);
        notification.setActorId(actorId);
        notification.setTargetUserId(targetUserId);
        notification.setNoticeType(noticeType);
        notification.setContent(content);
        notification.setCreatedAt(LocalDateTime.now());
        groupNotificationRepository.save(notification);
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (chatGroupRepository.existsByInviteCode(code));
        return code;
    }

    private PageRequest pageRequest(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return PageRequest.of(safePage - 1, safePageSize);
    }

    private PageResponse<GroupMessageResponse> toMessagePage(Page<GroupMessage> result, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            toMessageResponses(result.getContent()),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    private GroupResponse toGroupResponse(ChatGroup group) {
        long memberCount = groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(group.getId(), GroupMember.STATUS_ACTIVE);
        return new GroupResponse(group, memberCount);
    }

    private GroupResponse toGroupResponse(ChatGroup group, GroupMember member) {
        long memberCount = groupMemberRepository.countByGroupIdAndLeftAtIsNullAndStatus(group.getId(), GroupMember.STATUS_ACTIVE);
        long unreadCount = member.getLastReadAt() == null
            ? groupMessageRepository.countByGroupIdAndSenderIdNotAndDeletedAtIsNull(group.getId(), member.getUserId())
            : groupMessageRepository.countByGroupIdAndSenderIdNotAndDeletedAtIsNullAndSentAtAfter(
                group.getId(),
                member.getUserId(),
                member.getLastReadAt()
            );
        long mentionUnreadCount = member.getLastReadAt() == null
            ? groupMessageRepository.countUnreadMentions(group.getId(), member.getUserId())
            : groupMessageRepository.countUnreadMentionsAfter(group.getId(), member.getUserId(), member.getLastReadAt());
        boolean noticeUnread = group.getNoticeUpdatedAt() != null
            && (member.getNoticeReadAt() == null || member.getNoticeReadAt().isBefore(group.getNoticeUpdatedAt()));
        return new GroupResponse(group, memberCount, unreadCount, mentionUnreadCount, noticeUnread);
    }

    private List<GroupMessageResponse> toMessageResponses(List<GroupMessage> messages) {
        List<Long> senderIds = messages.stream().map(GroupMessage::getSenderId).distinct().toList();
        Map<Long, User> users = senderIds.isEmpty()
            ? Map.of()
            : userRepository.findAllById(senderIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> fileIds = messages.stream().map(GroupMessage::getFileId).filter(fileId -> fileId != null).distinct().toList();
        Map<Long, FileAsset> fileAssets = fileIds.isEmpty()
            ? Map.of()
            : fileAssetRepository.findAllById(fileIds).stream().collect(Collectors.toMap(FileAsset::getId, Function.identity()));
        List<Long> replyIds = messages.stream().map(GroupMessage::getReplyToMessageId).filter(id -> id != null).distinct().toList();
        Map<Long, GroupMessage> replies = replyIds.isEmpty()
            ? Map.of()
            : groupMessageRepository.findAllById(replyIds).stream().collect(Collectors.toMap(GroupMessage::getId, Function.identity()));
        Map<Long, List<Long>> mentionMap = messages.isEmpty()
            ? Map.of()
            : groupMessageMentionRepository.findByMessageIdIn(messages.stream().map(GroupMessage::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                    GroupMessageMention::getMessageId,
                    Collectors.mapping(GroupMessageMention::getUserId, Collectors.toList())
                ));
        return messages.stream()
            .map(message -> new GroupMessageResponse(
                message,
                users.get(message.getSenderId()),
                message.getFileId() == null ? null : fileAssets.get(message.getFileId()),
                message.getReplyToMessageId() == null ? null : replies.get(message.getReplyToMessageId()),
                mentionMap.getOrDefault(message.getId(), List.of())
            ))
            .toList();
    }
}

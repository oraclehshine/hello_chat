package com.hellochat.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellochat.backend.cache.GroupHotCacheService;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.entity.GroupMember;
import com.hellochat.backend.entity.GroupMessage;
import com.hellochat.backend.repository.GroupMemberRepository;
import com.hellochat.backend.repository.GroupMessageRepository;
import com.hellochat.backend.websocket.WebSocketDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LocalGroupPushDispatcher {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupHotCacheService groupHotCacheService;
    private final WebSocketDispatchPublisher webSocketDispatchPublisher;
    private final ObjectMapper objectMapper;

    public LocalGroupPushDispatcher(
        GroupMemberRepository groupMemberRepository,
        GroupMessageRepository groupMessageRepository,
        GroupHotCacheService groupHotCacheService,
        WebSocketDispatchPublisher webSocketDispatchPublisher,
        ObjectMapper objectMapper
    ) {
        this.groupMemberRepository = groupMemberRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupHotCacheService = groupHotCacheService;
        this.webSocketDispatchPublisher = webSocketDispatchPublisher;
        this.objectMapper = objectMapper;
    }

    public void pushNewMessage(Long groupId, Long senderId, GroupMessageResponse message) {
        List<GroupMember> members = activeMembers(groupId);
        for (GroupMember member : members) {
            Long userId = member.getUserId();
            refreshSummary(userId, groupId, message);
            if (userId != null && userId.equals(senderId)) {
                groupHotCacheService.resetUnreadCount(userId, groupId);
                groupHotCacheService.resetMentionUnreadCount(userId, groupId);
            } else {
                groupHotCacheService.incrementUnreadCount(userId, groupId);
                if (isMentioned(message, userId)) {
                    groupHotCacheService.incrementMentionUnreadCount(userId, groupId);
                }
            }
            push(userId, "group:message:new", message);
        }
    }

    public void pushMessageUpdated(Long groupId, Long messageId, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupId", groupId);
        payload.put("messageId", messageId);
        payload.put("eventType", eventType);
        refreshLatestSummary(groupId);
        for (GroupMember member : activeMembers(groupId)) {
            push(member.getUserId(), "group:message:update", payload);
        }
    }

    public void pushGroupUpdated(Long groupId, String eventType, GroupResponse group) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("group", group);
        for (GroupMember member : activeMembers(groupId)) {
            push(member.getUserId(), "group:update", payload);
        }
    }

    private List<GroupMember> activeMembers(Long groupId) {
        return groupMemberRepository.findByGroupIdAndLeftAtIsNullAndStatusOrderByRoleDescJoinedAtAsc(
            groupId,
            GroupMember.STATUS_ACTIVE
        );
    }

    private boolean isMentioned(GroupMessageResponse message, Long userId) {
        if (message == null || userId == null) {
            return false;
        }
        if (message.getMentionAll() != null && message.getMentionAll() == 1) {
            return true;
        }
        return message.getMentionUserIds() != null && message.getMentionUserIds().contains(userId);
    }

    private void refreshSummary(Long userId, Long groupId, GroupMessageResponse message) {
        if (userId == null || groupId == null || message == null) {
            return;
        }
        groupHotCacheService.cacheSummary(
            userId,
            groupId,
            message.getMessageId(),
            message.getMessageType(),
            buildPreview(message),
            message.getSentAt()
        );
    }

    private void refreshLatestSummary(Long groupId) {
        GroupMessage latestMessage = groupMessageRepository.findFirstByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(groupId).orElse(null);
        if (latestMessage == null) {
            return;
        }
        GroupMessageResponse latestResponse = new GroupMessageResponse(
            latestMessage.getId(),
            latestMessage.getGroupId(),
            latestMessage.getSenderId(),
            null,
            null,
            latestMessage.getMessageType(),
            latestMessage.getContent(),
            latestMessage.getFileId(),
            latestMessage.getReplyToMessageId(),
            null,
            List.of(),
            null,
            null,
            null,
            latestMessage.getMentionAll(),
            latestMessage.getRecallStatus(),
            latestMessage.getSentAt(),
            latestMessage.getUpdatedAt()
        );
        for (GroupMember member : activeMembers(groupId)) {
            refreshSummary(member.getUserId(), groupId, latestResponse);
        }
    }

    private String buildPreview(GroupMessageResponse message) {
        if (message.getRecallStatus() != null && message.getRecallStatus() == GroupMessage.RECALL_RECALLED) {
            return "Message recalled";
        }
        if ("text".equals(message.getMessageType())) {
            String content = message.getContent() == null ? "" : message.getContent();
            return content.length() > 80 ? content.substring(0, 80) + "..." : content;
        }
        return "[" + message.getMessageType() + "]";
    }

    private void push(Long userId, String eventType, Object payload) {
        if (userId == null || userId <= 0) {
            return;
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventType", eventType);
        message.put("userId", userId);
        message.put("payload", payload);
        try {
            webSocketDispatchPublisher.publish(userId, eventType, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize websocket payload", ex);
        }
    }
}

package com.hellochat.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.entity.GroupMember;
import com.hellochat.backend.repository.GroupMemberRepository;
import com.hellochat.backend.service.GroupPushService;
import com.hellochat.backend.websocket.WebSocketDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GroupPushServiceImpl implements GroupPushService {

    private final GroupMemberRepository groupMemberRepository;
    private final WebSocketDispatchPublisher webSocketDispatchPublisher;
    private final ObjectMapper objectMapper;

    public GroupPushServiceImpl(
        GroupMemberRepository groupMemberRepository,
        WebSocketDispatchPublisher webSocketDispatchPublisher,
        ObjectMapper objectMapper
    ) {
        this.groupMemberRepository = groupMemberRepository;
        this.webSocketDispatchPublisher = webSocketDispatchPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void pushNewMessage(Long groupId, GroupMessageResponse message) {
        pushToGroup(groupId, "group:message:new", message);
    }

    @Override
    public void pushMessageUpdated(Long groupId, Long messageId, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupId", groupId);
        payload.put("messageId", messageId);
        payload.put("eventType", eventType);
        pushToGroup(groupId, "group:message:update", payload);
    }

    @Override
    public void pushGroupUpdated(Long groupId, String eventType, GroupResponse group) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("group", group);
        pushToGroup(groupId, "group:update", payload);
    }

    private void pushToGroup(Long groupId, String eventType, Object payload) {
        groupMemberRepository.findByGroupIdAndLeftAtIsNullAndStatusOrderByRoleDescJoinedAtAsc(
            groupId,
            GroupMember.STATUS_ACTIVE
        ).forEach(member -> push(member.getUserId(), eventType, payload));
    }

    private void push(Long userId, String eventType, Object payload) {
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

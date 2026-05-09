package com.hellochat.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.entity.PrivateChat;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.repository.PrivateChatRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.ChatPushService;
import com.hellochat.backend.websocket.ChatWebSocketHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatPushServiceImpl implements ChatPushService {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final PrivateChatRepository privateChatRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ChatPushServiceImpl(
        ChatWebSocketHandler chatWebSocketHandler,
        PrivateChatRepository privateChatRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.privateChatRepository = privateChatRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void pushNewMessage(Long chatId, Long senderId, PrivateMessageResponse message) {
        PrivateChat chat = privateChatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            return;
        }
        Long targetUserId = chat.getUserAId().equals(senderId) ? chat.getUserBId() : chat.getUserAId();
        push(targetUserId, "message:new", message);
        push(senderId, "message:new", message);
    }

    @Override
    public void pushMessageUpdated(Long chatId, Long senderId, Long messageId, String eventType) {
        PrivateChat chat = privateChatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            return;
        }
        Long targetUserId = chat.getUserAId().equals(senderId) ? chat.getUserBId() : chat.getUserAId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("chatId", chatId);
        payload.put("messageId", messageId);
        push(targetUserId, "message:update", payload);
        push(senderId, "message:update", payload);
    }

    @Override
    public void pushReadReceipt(Long chatId, Long userId, Long lastReadMessageId) {
        PrivateChat chat = privateChatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            return;
        }
        Long targetUserId = chat.getUserAId().equals(userId) ? chat.getUserBId() : chat.getUserAId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chatId", chatId);
        payload.put("userId", userId);
        payload.put("lastReadMessageId", lastReadMessageId);
        push(targetUserId, "message:read", payload);
        push(userId, "message:read", payload);
    }

    @Override
    public void pushTypingStatus(Long chatId, Long userId, boolean typing) {
        PrivateChat chat = privateChatRepository.findById(chatId).orElse(null);
        if (chat == null) {
            return;
        }
        Long targetUserId = chat.getUserAId().equals(userId) ? chat.getUserBId() : chat.getUserAId();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chatId", chatId);
        payload.put("userId", userId);
        payload.put("typing", typing);
        push(targetUserId, "typing:update", payload);
    }

    private void push(Long userId, String eventType, Object payload) {
        if (userId == null || userId <= 0) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventType", eventType);
        message.put("userId", userId);
        message.put("payload", payload);
        try {
            chatWebSocketHandler.push(userId, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize websocket payload", ex);
        }
    }
}

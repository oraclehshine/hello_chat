package com.hellochat.backend.service.impl;

import com.hellochat.backend.config.HighConcurrencyProperties;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.service.ChatPushService;
import com.hellochat.backend.service.event.ChatPushEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatPushServiceImpl implements ChatPushService {

    private final KafkaTemplate<String, ChatPushEvent> kafkaTemplate;
    private final HighConcurrencyProperties properties;

    public ChatPushServiceImpl(
        KafkaTemplate<String, ChatPushEvent> kafkaTemplate,
        HighConcurrencyProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void pushNewMessage(Long chatId, Long senderId, PrivateMessageResponse message) {
        ChatPushEvent event = new ChatPushEvent();
        event.setType(ChatPushEvent.TYPE_NEW_MESSAGE);
        event.setChatId(chatId);
        event.setActorUserId(senderId);
        event.setMessage(message);
        publish(chatId, event);
    }

    @Override
    public void pushMessageUpdated(Long chatId, Long senderId, Long messageId, String eventType) {
        ChatPushEvent event = new ChatPushEvent();
        event.setType(ChatPushEvent.TYPE_MESSAGE_UPDATED);
        event.setChatId(chatId);
        event.setActorUserId(senderId);
        event.setMessageId(messageId);
        event.setEventType(eventType);
        publish(chatId, event);
    }

    @Override
    public void pushReadReceipt(Long chatId, Long userId, Long lastReadMessageId) {
        ChatPushEvent event = new ChatPushEvent();
        event.setType(ChatPushEvent.TYPE_READ_RECEIPT);
        event.setChatId(chatId);
        event.setActorUserId(userId);
        event.setLastReadMessageId(lastReadMessageId);
        publish(chatId, event);
    }

    @Override
    public void pushTypingStatus(Long chatId, Long userId, boolean typing) {
        ChatPushEvent event = new ChatPushEvent();
        event.setType(ChatPushEvent.TYPE_TYPING_STATUS);
        event.setChatId(chatId);
        event.setActorUserId(userId);
        event.setTyping(typing);
        publish(chatId, event);
    }

    private void publish(Long chatId, ChatPushEvent event) {
        kafkaTemplate.send(properties.getChatPushTopic(), String.valueOf(chatId), event);
    }
}

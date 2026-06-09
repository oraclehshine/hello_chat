package com.hellochat.backend.service;

import com.hellochat.backend.service.event.ChatPushEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ChatPushKafkaListener {

    private final LocalChatPushDispatcher localChatPushDispatcher;

    public ChatPushKafkaListener(LocalChatPushDispatcher localChatPushDispatcher) {
        this.localChatPushDispatcher = localChatPushDispatcher;
    }

    @KafkaListener(
        topics = "${hello-chat.concurrency.chat-push-topic}",
        groupId = "${spring.application.name}-chat-push"
    )
    public void consume(ChatPushEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        switch (event.getType()) {
            case ChatPushEvent.TYPE_NEW_MESSAGE ->
                localChatPushDispatcher.pushNewMessage(event.getChatId(), event.getActorUserId(), event.getMessage());
            case ChatPushEvent.TYPE_MESSAGE_UPDATED ->
                localChatPushDispatcher.pushMessageUpdated(
                    event.getChatId(),
                    event.getActorUserId(),
                    event.getMessageId(),
                    event.getEventType()
                );
            case ChatPushEvent.TYPE_READ_RECEIPT ->
                localChatPushDispatcher.pushReadReceipt(
                    event.getChatId(),
                    event.getActorUserId(),
                    event.getLastReadMessageId()
                );
            case ChatPushEvent.TYPE_TYPING_STATUS ->
                localChatPushDispatcher.pushTypingStatus(
                    event.getChatId(),
                    event.getActorUserId(),
                    Boolean.TRUE.equals(event.getTyping())
                );
            default -> {
            }
        }
    }
}

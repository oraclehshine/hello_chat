package com.hellochat.backend.service;

import com.hellochat.backend.dto.PrivateMessageResponse;

public interface ChatPushService {

    void pushNewMessage(Long chatId, Long senderId, PrivateMessageResponse message);

    void pushMessageUpdated(Long chatId, Long senderId, Long messageId, String eventType);

    void pushReadReceipt(Long chatId, Long userId, Long lastReadMessageId);

    void pushTypingStatus(Long chatId, Long userId, boolean typing);
}

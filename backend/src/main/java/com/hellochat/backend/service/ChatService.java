package com.hellochat.backend.service;

import com.hellochat.backend.dto.CreatePrivateChatRequest;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.PrivateChatResponse;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import java.util.List;

public interface ChatService {

    List<PrivateChatResponse> listChats(Long userId);

    PrivateChatResponse createOrGetPrivateChat(Long userId, CreatePrivateChatRequest request);

    PageResponse<PrivateMessageResponse> listMessages(Long userId, Long chatId, int page, int pageSize);

    PrivateMessageResponse sendMessage(Long userId, Long chatId, SendMessageRequest request);

    void recallMessage(Long userId, Long messageId);

    void deleteMessage(Long userId, Long messageId);

    void pinMessage(Long userId, Long messageId);

    void unpinMessage(Long userId, Long messageId);

    void markChatAsRead(Long userId, Long chatId);

    void updateTypingStatus(Long userId, Long chatId, boolean typing);

    PageResponse<PrivateMessageResponse> searchMessages(Long userId, Long chatId, String keyword, int page, int pageSize);
}

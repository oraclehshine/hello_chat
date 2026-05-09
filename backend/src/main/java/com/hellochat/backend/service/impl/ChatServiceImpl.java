package com.hellochat.backend.service.impl;

import com.hellochat.backend.dto.CreatePrivateChatRequest;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.PrivateChatResponse;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.PrivateChat;
import com.hellochat.backend.entity.PrivateMessage;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.repository.FileAssetRepository;
import com.hellochat.backend.repository.FriendshipRepository;
import com.hellochat.backend.repository.PrivateChatRepository;
import com.hellochat.backend.repository.PrivateMessageRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.ChatPushService;
import com.hellochat.backend.service.ChatService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Set<String> SUPPORTED_MESSAGE_TYPES = Set.of("text", "image", "file");

    private final PrivateChatRepository privateChatRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final UserRepository userRepository;
    private final FileAssetRepository fileAssetRepository;
    private final FriendshipRepository friendshipRepository;
    private final ChatPushService chatPushService;

    public ChatServiceImpl(
        PrivateChatRepository privateChatRepository,
        PrivateMessageRepository privateMessageRepository,
        UserRepository userRepository,
        FileAssetRepository fileAssetRepository,
        FriendshipRepository friendshipRepository,
        ChatPushService chatPushService
    ) {
        this.privateChatRepository = privateChatRepository;
        this.privateMessageRepository = privateMessageRepository;
        this.userRepository = userRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.friendshipRepository = friendshipRepository;
        this.chatPushService = chatPushService;
    }

    @Override
    public List<PrivateChatResponse> listChats(Long userId) {
        requireUser(userId);
        return privateChatRepository.findByUserAIdOrUserBIdOrderByLastMessageAtDescUpdatedAtDesc(userId, userId)
            .stream()
            .map(chat -> toChatResponse(userId, chat))
            .toList();
    }

    @Override
    @Transactional
    public PrivateChatResponse createOrGetPrivateChat(Long userId, CreatePrivateChatRequest request) {
        requireUser(userId);
        requireUser(request.getTargetUserId());
        if (userId.equals(request.getTargetUserId())) {
            throw new IllegalArgumentException("target user invalid");
        }
        requireFriend(userId, request.getTargetUserId());
        long userAId = Math.min(userId, request.getTargetUserId());
        long userBId = Math.max(userId, request.getTargetUserId());
        PrivateChat chat = privateChatRepository.findByUserAIdAndUserBId(userAId, userBId)
            .orElseGet(() -> {
                PrivateChat created = new PrivateChat();
                created.setUserAId(userAId);
                created.setUserBId(userBId);
                created.setCreatedAt(LocalDateTime.now());
                created.setUpdatedAt(LocalDateTime.now());
                return privateChatRepository.save(created);
            });
        return toChatResponse(userId, chat);
    }

    @Override
    public PageResponse<PrivateMessageResponse> listMessages(Long userId, Long chatId, int page, int pageSize) {
        requireChatMember(userId, chatId);
        Page<PrivateMessage> result = privateMessageRepository
            .findByChatIdAndDeletedAtIsNullOrderBySentAtDesc(chatId, pageRequest(page, pageSize));
        return toMessagePage(result, page, pageSize);
    }

    @Override
    @Transactional
    public PrivateMessageResponse sendMessage(Long userId, Long chatId, SendMessageRequest request) {
        PrivateChat chat = requireChatMember(userId, chatId);
        if (!SUPPORTED_MESSAGE_TYPES.contains(request.getMessageType())) {
            throw new IllegalArgumentException("message type unsupported");
        }
        if ("text".equals(request.getMessageType()) && (request.getContent() == null || request.getContent().isBlank())) {
            throw new IllegalArgumentException("message content is required");
        }
        if ("image".equals(request.getMessageType()) || "file".equals(request.getMessageType())) {
            if (request.getFileId() == null) {
                throw new IllegalArgumentException("fileId is required");
            }
            FileAsset fileAsset = fileAssetRepository.findById(request.getFileId())
                .orElseThrow(() -> new IllegalArgumentException("file not found"));
            if (!fileAsset.getUploaderId().equals(userId)) {
                throw new IllegalArgumentException("permission denied");
            }
            request.setContent(fileAsset.getFileUrl());
        }
        PrivateMessage message = new PrivateMessage();
        message.setChatId(chatId);
        message.setSenderId(userId);
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setFileId(request.getFileId());
        message.setSentAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        PrivateMessage saved = privateMessageRepository.save(message);

        chat.setLastMessageId(saved.getId());
        chat.setLastMessageAt(saved.getSentAt());
        chat.setUpdatedAt(LocalDateTime.now());
        privateChatRepository.save(chat);
        FileAsset responseFileAsset = saved.getFileId() == null ? null : fileAssetRepository.findById(saved.getFileId()).orElse(null);
        PrivateMessageResponse response = new PrivateMessageResponse(saved, responseFileAsset);
        chatPushService.pushNewMessage(chatId, userId, response);
        return response;
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        PrivateMessage message = requireMessageSender(userId, messageId);
        if (message.getSentAt().isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new IllegalArgumentException("message recall window expired");
        }
        message.setRecallStatus(PrivateMessage.RECALL_RECALLED);
        message.setContent("");
        message.setUpdatedAt(LocalDateTime.now());
        privateMessageRepository.save(message);
        chatPushService.pushMessageUpdated(message.getChatId(), userId, messageId, "message:recalled");
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        requireChatMember(userId, message.getChatId());
        message.setDeletedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        privateMessageRepository.save(message);
        chatPushService.pushMessageUpdated(message.getChatId(), userId, messageId, "message:deleted");
    }

    @Override
    @Transactional
    public void pinMessage(Long userId, Long messageId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        requireChatMember(userId, message.getChatId());
        message.setPinnedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        privateMessageRepository.save(message);
        chatPushService.pushMessageUpdated(message.getChatId(), userId, messageId, "message:pinned");
    }

    @Override
    @Transactional
    public void unpinMessage(Long userId, Long messageId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        requireChatMember(userId, message.getChatId());
        message.setPinnedAt(null);
        message.setUpdatedAt(LocalDateTime.now());
        privateMessageRepository.save(message);
        chatPushService.pushMessageUpdated(message.getChatId(), userId, messageId, "message:unpinned");
    }

    @Override
    public PageResponse<PrivateMessageResponse> searchMessages(Long userId, Long chatId, String keyword, int page, int pageSize) {
        requireChatMember(userId, chatId);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<PrivateMessage> result = privateMessageRepository
            .findByChatIdAndContentContainingIgnoreCaseAndDeletedAtIsNullOrderBySentAtDesc(
                chatId,
                safeKeyword,
                pageRequest(page, pageSize)
        );
        return toMessagePage(result, page, pageSize);
    }

    @Override
    @Transactional
    public void markChatAsRead(Long userId, Long chatId) {
        requireChatMember(userId, chatId);
        int updated = privateMessageRepository.markChatMessagesRead(
            chatId,
            userId,
            PrivateMessage.STATUS_READ,
            LocalDateTime.now()
        );
        if (updated > 0) {
            Long lastReadMessageId = privateMessageRepository
                .findFirstByChatIdAndSenderIdNotAndDeletedAtIsNullOrderBySentAtDesc(chatId, userId)
                .map(PrivateMessage::getId)
                .orElse(null);
            chatPushService.pushReadReceipt(chatId, userId, lastReadMessageId);
        }
    }

    @Override
    public void updateTypingStatus(Long userId, Long chatId, boolean typing) {
        requireChatMember(userId, chatId);
        chatPushService.pushTypingStatus(chatId, userId, typing);
    }

    private PrivateChat requireChatMember(Long userId, Long chatId) {
        PrivateChat chat = privateChatRepository.findById(chatId)
            .orElseThrow(() -> new IllegalArgumentException("chat not found"));
        if (!chat.getUserAId().equals(userId) && !chat.getUserBId().equals(userId)) {
            throw new IllegalArgumentException("permission denied");
        }
        return chat;
    }

    private PrivateMessage requireMessageSender(Long userId, Long messageId) {
        PrivateMessage message = privateMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("message not found"));
        requireChatMember(userId, message.getChatId());
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
            .orElseThrow(() -> new IllegalArgumentException("only friends can start private chat"));
    }

    private PrivateChatResponse toChatResponse(Long currentUserId, PrivateChat chat) {
        Long targetUserId = chat.getUserAId().equals(currentUserId) ? chat.getUserBId() : chat.getUserAId();
        PrivateMessage lastMessage = null;
        if (chat.getLastMessageId() != null) {
            lastMessage = privateMessageRepository.findById(chat.getLastMessageId()).orElse(null);
        }
        long unreadCount = privateMessageRepository.countByChatIdAndSenderIdNotAndMessageStatusAndDeletedAtIsNull(
            chat.getId(),
            currentUserId,
            PrivateMessage.STATUS_SENT
        );
        return new PrivateChatResponse(chat, new UserProfileResponse(requireUser(targetUserId)), lastMessage, unreadCount);
    }

    private PageRequest pageRequest(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return PageRequest.of(safePage - 1, safePageSize);
    }

    private PageResponse<PrivateMessageResponse> toMessagePage(Page<PrivateMessage> result, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return new PageResponse<>(
            toMessageResponses(result.getContent()),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    private List<PrivateMessageResponse> toMessageResponses(List<PrivateMessage> messages) {
        List<Long> fileIds = messages.stream()
            .map(PrivateMessage::getFileId)
            .filter(fileId -> fileId != null)
            .distinct()
            .toList();
        Map<Long, FileAsset> fileAssets = fileIds.isEmpty()
            ? Map.of()
            : fileAssetRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity()));
        return messages.stream()
            .map(message -> new PrivateMessageResponse(message, fileAssets.get(message.getFileId())))
            .toList();
    }
}

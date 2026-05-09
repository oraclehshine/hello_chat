package com.hellochat.backend.controller;

import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.CreatePrivateChatRequest;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.PrivateChatResponse;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.TypingStatusRequest;
import com.hellochat.backend.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;
    private final TokenProvider tokenProvider;

    public ChatController(ChatService chatService, TokenProvider tokenProvider) {
        this.chatService = chatService;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/chats")
    public ApiResponse<List<PrivateChatResponse>> listChats(HttpServletRequest request) {
        return ApiResponse.success(chatService.listChats(currentUserId(request)));
    }

    @PostMapping("/chats/private")
    public ApiResponse<PrivateChatResponse> createPrivateChat(
        HttpServletRequest request,
        @Valid @RequestBody CreatePrivateChatRequest chatRequest
    ) {
        return ApiResponse.success(chatService.createOrGetPrivateChat(currentUserId(request), chatRequest));
    }

    @GetMapping("/chats/{chatId}/messages")
    public ApiResponse<PageResponse<PrivateMessageResponse>> listMessages(
        HttpServletRequest request,
        @PathVariable Long chatId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(chatService.listMessages(currentUserId(request), chatId, page, pageSize));
    }

    @PostMapping("/chats/{chatId}/messages")
    public ApiResponse<PrivateMessageResponse> sendMessage(
        HttpServletRequest request,
        @PathVariable Long chatId,
        @Valid @RequestBody SendMessageRequest messageRequest
    ) {
        return ApiResponse.success(chatService.sendMessage(currentUserId(request), chatId, messageRequest));
    }

    @PostMapping("/messages/{messageId}/recall")
    public ApiResponse<Void> recallMessage(HttpServletRequest request, @PathVariable Long messageId) {
        chatService.recallMessage(currentUserId(request), messageId);
        return ApiResponse.success();
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(HttpServletRequest request, @PathVariable Long messageId) {
        chatService.deleteMessage(currentUserId(request), messageId);
        return ApiResponse.success();
    }

    @GetMapping("/messages/search")
    public ApiResponse<PageResponse<PrivateMessageResponse>> searchMessages(
        HttpServletRequest request,
        @RequestParam Long chatId,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(chatService.searchMessages(currentUserId(request), chatId, keyword, page, pageSize));
    }

    @PostMapping("/messages/{messageId}/pin")
    public ApiResponse<Void> pinMessage(HttpServletRequest request, @PathVariable Long messageId) {
        chatService.pinMessage(currentUserId(request), messageId);
        return ApiResponse.success();
    }

    @DeleteMapping("/messages/{messageId}/pin")
    public ApiResponse<Void> unpinMessage(HttpServletRequest request, @PathVariable Long messageId) {
        chatService.unpinMessage(currentUserId(request), messageId);
        return ApiResponse.success();
    }

    @PostMapping("/chats/{chatId}/read")
    public ApiResponse<Void> markChatAsRead(HttpServletRequest request, @PathVariable Long chatId) {
        chatService.markChatAsRead(currentUserId(request), chatId);
        return ApiResponse.success();
    }

    @PostMapping("/chats/{chatId}/typing")
    public ApiResponse<Void> updateTypingStatus(
        HttpServletRequest request,
        @PathVariable Long chatId,
        @RequestBody TypingStatusRequest typingStatusRequest
    ) {
        chatService.updateTypingStatus(currentUserId(request), chatId, typingStatusRequest.isTyping());
        return ApiResponse.success();
    }

    private Long currentUserId(HttpServletRequest request) {
        return CurrentUser.requireUserId(request, tokenProvider);
    }
}

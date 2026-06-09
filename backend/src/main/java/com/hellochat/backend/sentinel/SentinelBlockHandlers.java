package com.hellochat.backend.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.dto.AuthResponse;
import com.hellochat.backend.dto.CaptchaRequest;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.PrivateMessageResponse;
import com.hellochat.backend.dto.LoginRequest;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.UploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public final class SentinelBlockHandlers {

    private static final int TOO_MANY_REQUESTS = 42900;

    private SentinelBlockHandlers() {
    }

    public static ApiResponse<String> blockedSendCaptcha(CaptchaRequest request, BlockException ex) {
        return limit("captcha requests are too frequent");
    }

    public static ApiResponse<String> blockedSendCaptchaByGet(String email, String scene, BlockException ex) {
        return limit("captcha requests are too frequent");
    }

    public static ApiResponse<AuthResponse> blockedLogin(LoginRequest request, BlockException ex) {
        return limit("login requests are too frequent");
    }

    public static ApiResponse<PrivateMessageResponse> blockedChatSendMessage(
        HttpServletRequest request,
        Long chatId,
        SendMessageRequest messageRequest,
        BlockException ex
    ) {
        return limit("chat message requests are too frequent");
    }

    public static ApiResponse<GroupMessageResponse> blockedGroupSendMessage(
        HttpServletRequest request,
        Long groupId,
        SendMessageRequest sendMessageRequest,
        BlockException ex
    ) {
        return limit("group message requests are too frequent");
    }

    public static ApiResponse<GroupMessageResponse> blockedGroupMentionAll(
        HttpServletRequest request,
        Long groupId,
        SendMessageRequest sendMessageRequest,
        BlockException ex
    ) {
        return limit("mention-all requests are too frequent");
    }

    public static ApiResponse<UploadResponse> blockedFileUpload(
        HttpServletRequest request,
        MultipartFile file,
        String scene,
        BlockException ex
    ) {
        return limit("file upload requests are too frequent");
    }

    private static <T> ApiResponse<T> limit(String message) {
        return ApiResponse.failure(TOO_MANY_REQUESTS, message);
    }
}

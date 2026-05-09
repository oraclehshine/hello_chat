package com.hellochat.backend.controller;

import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.CreateFriendRequestRequest;
import com.hellochat.backend.dto.FriendRequestResponse;
import com.hellochat.backend.dto.FriendResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.UpdateEmailRequest;
import com.hellochat.backend.dto.UpdateFriendRequest;
import com.hellochat.backend.dto.UpdateProfileRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final TokenProvider tokenProvider;

    public UserController(UserService userService, TokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(HttpServletRequest request) {
        return ApiResponse.success(userService.getMe(CurrentUser.requireUserId(request, tokenProvider)));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(
        HttpServletRequest request,
        @Valid @RequestBody UpdateProfileRequest updateRequest
    ) {
        return ApiResponse.success(userService.updateProfile(CurrentUser.requireUserId(request, tokenProvider), updateRequest));
    }

    @PutMapping("/me/email")
    public ApiResponse<UserProfileResponse> updateEmail(
        HttpServletRequest request,
        @Valid @RequestBody UpdateEmailRequest updateRequest
    ) {
        return ApiResponse.success(userService.updateEmail(CurrentUser.requireUserId(request, tokenProvider), updateRequest));
    }

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<UserProfileResponse>> search(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(userService.search(keyword, page, pageSize));
    }

    @GetMapping("/friends")
    public ApiResponse<List<FriendResponse>> listFriends(HttpServletRequest request) {
        return ApiResponse.success(userService.listFriends(CurrentUser.requireUserId(request, tokenProvider)));
    }

    @PutMapping("/friends/{friendUserId}")
    public ApiResponse<FriendResponse> updateFriend(
        HttpServletRequest request,
        @PathVariable Long friendUserId,
        @Valid @RequestBody UpdateFriendRequest updateFriendRequest
    ) {
        return ApiResponse.success(userService.updateFriend(
            CurrentUser.requireUserId(request, tokenProvider),
            friendUserId,
            updateFriendRequest
        ));
    }

    @DeleteMapping("/friends/{friendUserId}")
    public ApiResponse<Void> deleteFriend(HttpServletRequest request, @PathVariable Long friendUserId) {
        userService.deleteFriend(CurrentUser.requireUserId(request, tokenProvider), friendUserId);
        return ApiResponse.success();
    }

    @PostMapping("/friend-requests")
    public ApiResponse<FriendRequestResponse> sendFriendRequest(
        HttpServletRequest request,
        @Valid @RequestBody CreateFriendRequestRequest createFriendRequestRequest
    ) {
        return ApiResponse.success(userService.sendFriendRequest(
            CurrentUser.requireUserId(request, tokenProvider),
            createFriendRequestRequest
        ));
    }

    @GetMapping("/friend-requests/received")
    public ApiResponse<List<FriendRequestResponse>> listReceivedFriendRequests(HttpServletRequest request) {
        return ApiResponse.success(userService.listReceivedFriendRequests(CurrentUser.requireUserId(request, tokenProvider)));
    }

    @GetMapping("/friend-requests/sent")
    public ApiResponse<List<FriendRequestResponse>> listSentFriendRequests(HttpServletRequest request) {
        return ApiResponse.success(userService.listSentFriendRequests(CurrentUser.requireUserId(request, tokenProvider)));
    }

    @PostMapping("/friend-requests/{requestId}/approve")
    public ApiResponse<FriendResponse> approveFriendRequest(HttpServletRequest request, @PathVariable Long requestId) {
        return ApiResponse.success(userService.approveFriendRequest(CurrentUser.requireUserId(request, tokenProvider), requestId));
    }

    @PostMapping("/friend-requests/{requestId}/reject")
    public ApiResponse<Void> rejectFriendRequest(HttpServletRequest request, @PathVariable Long requestId) {
        userService.rejectFriendRequest(CurrentUser.requireUserId(request, tokenProvider), requestId);
        return ApiResponse.success();
    }

    @PostMapping("/blocks/{blockedUserId}")
    public ApiResponse<Void> blockUser(HttpServletRequest request, @PathVariable Long blockedUserId) {
        userService.blockUser(CurrentUser.requireUserId(request, tokenProvider), blockedUserId);
        return ApiResponse.success();
    }

    @DeleteMapping("/blocks/{blockedUserId}")
    public ApiResponse<Void> unblockUser(HttpServletRequest request, @PathVariable Long blockedUserId) {
        userService.unblockUser(CurrentUser.requireUserId(request, tokenProvider), blockedUserId);
        return ApiResponse.success();
    }

    @GetMapping("/blocks")
    public ApiResponse<List<UserProfileResponse>> listBlockedUsers(HttpServletRequest request) {
        return ApiResponse.success(userService.listBlockedUsers(CurrentUser.requireUserId(request, tokenProvider)));
    }
}

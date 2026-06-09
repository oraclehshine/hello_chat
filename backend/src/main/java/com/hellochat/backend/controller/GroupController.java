package com.hellochat.backend.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.AddGroupMembersRequest;
import com.hellochat.backend.dto.CreateGroupRequest;
import com.hellochat.backend.dto.GroupJoinRequestRequest;
import com.hellochat.backend.dto.GroupJoinRequestResponse;
import com.hellochat.backend.dto.GroupMemberResponse;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupNoticeReadResponse;
import com.hellochat.backend.dto.GroupNotificationResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.dto.MuteGroupMemberRequest;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.TransferGroupOwnerRequest;
import com.hellochat.backend.dto.UpdateGroupNoticeRequest;
import com.hellochat.backend.dto.UpdateGroupNicknameRequest;
import com.hellochat.backend.dto.UpdateGroupRequest;
import com.hellochat.backend.sentinel.SentinelBlockHandlers;
import com.hellochat.backend.sentinel.SentinelResourceNames;
import com.hellochat.backend.service.GroupService;
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
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final TokenProvider tokenProvider;

    public GroupController(GroupService groupService, TokenProvider tokenProvider) {
        this.groupService = groupService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping
    public ApiResponse<GroupResponse> createGroup(
        HttpServletRequest request,
        @Valid @RequestBody CreateGroupRequest createGroupRequest
    ) {
        return ApiResponse.success(groupService.createGroup(currentUserId(request), createGroupRequest));
    }

    @GetMapping
    public ApiResponse<List<GroupResponse>> listMyGroups(HttpServletRequest request) {
        return ApiResponse.success(groupService.listMyGroups(currentUserId(request)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<GroupResponse>> searchGroups(
        HttpServletRequest request,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(groupService.searchGroups(currentUserId(request), keyword, page, pageSize));
    }

    @PostMapping("/invite/{inviteCode}/join")
    public ApiResponse<GroupResponse> joinByInviteCode(HttpServletRequest request, @PathVariable String inviteCode) {
        return ApiResponse.success(groupService.joinByInviteCode(currentUserId(request), inviteCode));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroup(HttpServletRequest request, @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroup(currentUserId(request), groupId));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<GroupResponse> updateGroup(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody UpdateGroupRequest updateGroupRequest
    ) {
        return ApiResponse.success(groupService.updateGroup(currentUserId(request), groupId, updateGroupRequest));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> dissolveGroup(HttpServletRequest request, @PathVariable Long groupId) {
        groupService.dissolveGroup(currentUserId(request), groupId);
        return ApiResponse.success();
    }

    @PostMapping("/{groupId}/leave")
    public ApiResponse<Void> leaveGroup(HttpServletRequest request, @PathVariable Long groupId) {
        groupService.leaveGroup(currentUserId(request), groupId);
        return ApiResponse.success();
    }

    @PostMapping("/{groupId}/owner")
    public ApiResponse<List<GroupMemberResponse>> transferOwner(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody TransferGroupOwnerRequest transferGroupOwnerRequest
    ) {
        return ApiResponse.success(groupService.transferOwner(
            currentUserId(request),
            groupId,
            transferGroupOwnerRequest.getTargetUserId()
        ));
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberResponse>> listMembers(HttpServletRequest request, @PathVariable Long groupId) {
        return ApiResponse.success(groupService.listMembers(currentUserId(request), groupId));
    }

    @PostMapping("/{groupId}/join-requests")
    public ApiResponse<GroupJoinRequestResponse> requestJoinGroup(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody GroupJoinRequestRequest groupJoinRequestRequest
    ) {
        return ApiResponse.success(groupService.requestJoinGroup(currentUserId(request), groupId, groupJoinRequestRequest));
    }

    @GetMapping("/{groupId}/join-requests")
    public ApiResponse<List<GroupJoinRequestResponse>> listJoinRequests(
        HttpServletRequest request,
        @PathVariable Long groupId
    ) {
        return ApiResponse.success(groupService.listJoinRequests(currentUserId(request), groupId));
    }

    @GetMapping("/join-requests/me")
    public ApiResponse<List<GroupJoinRequestResponse>> listMyJoinRequests(HttpServletRequest request) {
        return ApiResponse.success(groupService.listMyJoinRequests(currentUserId(request)));
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/approve")
    public ApiResponse<List<GroupMemberResponse>> approveJoinRequest(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long requestId
    ) {
        return ApiResponse.success(groupService.reviewJoinRequest(currentUserId(request), groupId, requestId, true));
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/reject")
    public ApiResponse<List<GroupMemberResponse>> rejectJoinRequest(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long requestId
    ) {
        return ApiResponse.success(groupService.reviewJoinRequest(currentUserId(request), groupId, requestId, false));
    }

    @GetMapping("/{groupId}/notifications")
    public ApiResponse<PageResponse<GroupNotificationResponse>> listNotifications(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(groupService.listNotifications(currentUserId(request), groupId, page, pageSize));
    }

    @PutMapping("/{groupId}/members/me/nickname")
    public ApiResponse<List<GroupMemberResponse>> updateMyNickname(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody UpdateGroupNicknameRequest updateGroupNicknameRequest
    ) {
        return ApiResponse.success(groupService.updateMyNickname(currentUserId(request), groupId, updateGroupNicknameRequest));
    }

    @GetMapping("/{groupId}/messages")
    public ApiResponse<PageResponse<GroupMessageResponse>> listMessages(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(groupService.listMessages(currentUserId(request), groupId, page, pageSize));
    }

    @PostMapping("/{groupId}/read")
    public ApiResponse<Void> markGroupAsRead(HttpServletRequest request, @PathVariable Long groupId) {
        groupService.markGroupAsRead(currentUserId(request), groupId);
        return ApiResponse.success();
    }

    @GetMapping("/{groupId}/messages/search")
    public ApiResponse<PageResponse<GroupMessageResponse>> searchMessages(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(groupService.searchMessages(currentUserId(request), groupId, keyword, page, pageSize));
    }

    @GetMapping("/{groupId}/files")
    public ApiResponse<PageResponse<GroupMessageResponse>> listFiles(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(groupService.listFiles(currentUserId(request), groupId, page, pageSize));
    }

    @PostMapping("/{groupId}/messages")
    @SentinelResource(
        value = SentinelResourceNames.GROUP_SEND_MESSAGE,
        blockHandlerClass = SentinelBlockHandlers.class,
        blockHandler = "blockedGroupSendMessage"
    )
    public ApiResponse<GroupMessageResponse> sendMessage(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody SendMessageRequest sendMessageRequest
    ) {
        return ApiResponse.success(groupService.sendMessage(currentUserId(request), groupId, sendMessageRequest));
    }

    @PostMapping("/{groupId}/messages/{messageId}/recall")
    public ApiResponse<Void> recallMessage(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long messageId
    ) {
        groupService.recallMessage(currentUserId(request), groupId, messageId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{groupId}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long messageId
    ) {
        groupService.deleteMessage(currentUserId(request), groupId, messageId);
        return ApiResponse.success();
    }

    @PostMapping("/{groupId}/messages/mention-all")
    @SentinelResource(
        value = SentinelResourceNames.GROUP_SEND_MENTION_ALL,
        blockHandlerClass = SentinelBlockHandlers.class,
        blockHandler = "blockedGroupMentionAll"
    )
    public ApiResponse<GroupMessageResponse> sendMentionAllMessage(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody SendMessageRequest sendMessageRequest
    ) {
        sendMessageRequest.setMentionAll(true);
        return ApiResponse.success(groupService.sendMessage(currentUserId(request), groupId, sendMessageRequest));
    }

    @PostMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberResponse>> addMembers(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody AddGroupMembersRequest addGroupMembersRequest
    ) {
        return ApiResponse.success(groupService.addMembers(currentUserId(request), groupId, addGroupMembersRequest.getMemberIds()));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<Void> removeMember(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        groupService.removeMember(currentUserId(request), groupId, userId);
        return ApiResponse.success();
    }

    @PostMapping("/{groupId}/announcement")
    public ApiResponse<GroupResponse> updateNotice(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @Valid @RequestBody UpdateGroupNoticeRequest updateGroupNoticeRequest
    ) {
        return ApiResponse.success(groupService.updateNotice(currentUserId(request), groupId, updateGroupNoticeRequest));
    }

    @GetMapping("/{groupId}/announcement")
    public ApiResponse<GroupResponse> getNotice(HttpServletRequest request, @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroup(currentUserId(request), groupId));
    }

    @PostMapping("/{groupId}/announcement/read")
    public ApiResponse<GroupNoticeReadResponse> markNoticeRead(HttpServletRequest request, @PathVariable Long groupId) {
        return ApiResponse.success(groupService.markNoticeRead(currentUserId(request), groupId));
    }

    @GetMapping("/{groupId}/announcement/read-stats")
    public ApiResponse<GroupNoticeReadResponse> getNoticeReadStats(HttpServletRequest request, @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getNoticeReadStats(currentUserId(request), groupId));
    }

    @PostMapping("/{groupId}/members/{userId}/admin")
    public ApiResponse<List<GroupMemberResponse>> setAdmin(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        return ApiResponse.success(groupService.setAdmin(currentUserId(request), groupId, userId, true));
    }

    @DeleteMapping("/{groupId}/members/{userId}/admin")
    public ApiResponse<List<GroupMemberResponse>> unsetAdmin(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        return ApiResponse.success(groupService.setAdmin(currentUserId(request), groupId, userId, false));
    }

    @PostMapping("/{groupId}/members/{userId}/mute")
    public ApiResponse<List<GroupMemberResponse>> muteMember(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long userId,
        @Valid @RequestBody MuteGroupMemberRequest muteGroupMemberRequest
    ) {
        return ApiResponse.success(groupService.muteMember(
            currentUserId(request),
            groupId,
            userId,
            muteGroupMemberRequest.getMinutes()
        ));
    }

    @DeleteMapping("/{groupId}/members/{userId}/mute")
    public ApiResponse<List<GroupMemberResponse>> unmuteMember(
        HttpServletRequest request,
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        return ApiResponse.success(groupService.unmuteMember(currentUserId(request), groupId, userId));
    }

    private Long currentUserId(HttpServletRequest request) {
        return CurrentUser.requireUserId(request, tokenProvider);
    }
}

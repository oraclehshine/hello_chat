package com.hellochat.backend.service;

import com.hellochat.backend.dto.CreateGroupRequest;
import com.hellochat.backend.dto.GroupMemberResponse;
import com.hellochat.backend.dto.GroupMessageResponse;
import com.hellochat.backend.dto.GroupJoinRequestRequest;
import com.hellochat.backend.dto.GroupJoinRequestResponse;
import com.hellochat.backend.dto.GroupNoticeReadResponse;
import com.hellochat.backend.dto.GroupNotificationResponse;
import com.hellochat.backend.dto.GroupResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.SendMessageRequest;
import com.hellochat.backend.dto.UpdateGroupNoticeRequest;
import com.hellochat.backend.dto.UpdateGroupRequest;
import com.hellochat.backend.dto.UpdateGroupNicknameRequest;
import java.util.List;

public interface GroupService {

    GroupResponse createGroup(Long userId, CreateGroupRequest request);

    List<GroupResponse> listMyGroups(Long userId);

    GroupResponse getGroup(Long userId, Long groupId);

    PageResponse<GroupResponse> searchGroups(Long userId, String keyword, int page, int pageSize);

    GroupResponse joinByInviteCode(Long userId, String inviteCode);

    GroupJoinRequestResponse requestJoinGroup(Long userId, Long groupId, GroupJoinRequestRequest request);

    List<GroupJoinRequestResponse> listJoinRequests(Long userId, Long groupId);

    List<GroupJoinRequestResponse> listMyJoinRequests(Long userId);

    List<GroupMemberResponse> reviewJoinRequest(Long userId, Long groupId, Long requestId, boolean approve);

    PageResponse<GroupNotificationResponse> listNotifications(Long userId, Long groupId, int page, int pageSize);

    List<GroupMemberResponse> listMembers(Long userId, Long groupId);

    PageResponse<GroupMessageResponse> listMessages(Long userId, Long groupId, int page, int pageSize);

    void markGroupAsRead(Long userId, Long groupId);

    PageResponse<GroupMessageResponse> searchMessages(Long userId, Long groupId, String keyword, int page, int pageSize);

    PageResponse<GroupMessageResponse> listFiles(Long userId, Long groupId, int page, int pageSize);

    GroupMessageResponse sendMessage(Long userId, Long groupId, SendMessageRequest request);

    void recallMessage(Long userId, Long groupId, Long messageId);

    void deleteMessage(Long userId, Long groupId, Long messageId);

    List<GroupMemberResponse> addMembers(Long userId, Long groupId, List<Long> memberIds);

    void removeMember(Long userId, Long groupId, Long memberUserId);

    GroupResponse updateNotice(Long userId, Long groupId, UpdateGroupNoticeRequest request);

    GroupNoticeReadResponse markNoticeRead(Long userId, Long groupId);

    GroupNoticeReadResponse getNoticeReadStats(Long userId, Long groupId);

    GroupResponse updateGroup(Long userId, Long groupId, UpdateGroupRequest request);

    void leaveGroup(Long userId, Long groupId);

    void dissolveGroup(Long userId, Long groupId);

    List<GroupMemberResponse> transferOwner(Long userId, Long groupId, Long targetUserId);

    List<GroupMemberResponse> updateMyNickname(Long userId, Long groupId, UpdateGroupNicknameRequest request);

    List<GroupMemberResponse> setAdmin(Long userId, Long groupId, Long memberUserId, boolean admin);

    List<GroupMemberResponse> muteMember(Long userId, Long groupId, Long memberUserId, int minutes);

    List<GroupMemberResponse> unmuteMember(Long userId, Long groupId, Long memberUserId);
}

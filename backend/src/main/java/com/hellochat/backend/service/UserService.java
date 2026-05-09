package com.hellochat.backend.service;

import com.hellochat.backend.dto.CreateFriendRequestRequest;
import com.hellochat.backend.dto.FriendRequestResponse;
import com.hellochat.backend.dto.FriendResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.UpdateEmailRequest;
import com.hellochat.backend.dto.UpdateFriendRequest;
import com.hellochat.backend.dto.UpdateProfileRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import java.util.List;

public interface UserService {

    UserProfileResponse getMe(Long userId);

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileResponse updateEmail(Long userId, UpdateEmailRequest request);

    PageResponse<UserProfileResponse> search(String keyword, int page, int pageSize);

    List<FriendResponse> listFriends(Long userId);

    List<FriendRequestResponse> listReceivedFriendRequests(Long userId);

    List<FriendRequestResponse> listSentFriendRequests(Long userId);

    FriendRequestResponse sendFriendRequest(Long userId, CreateFriendRequestRequest request);

    FriendResponse approveFriendRequest(Long userId, Long requestId);

    void rejectFriendRequest(Long userId, Long requestId);

    FriendResponse updateFriend(Long userId, Long friendUserId, UpdateFriendRequest request);

    void deleteFriend(Long userId, Long friendUserId);

    void blockUser(Long userId, Long blockedUserId);

    void unblockUser(Long userId, Long blockedUserId);

    List<UserProfileResponse> listBlockedUsers(Long userId);
}

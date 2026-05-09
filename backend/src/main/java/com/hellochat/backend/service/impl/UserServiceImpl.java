package com.hellochat.backend.service.impl;

import com.hellochat.backend.dto.CreateFriendRequestRequest;
import com.hellochat.backend.dto.FriendRequestResponse;
import com.hellochat.backend.dto.FriendResponse;
import com.hellochat.backend.dto.PageResponse;
import com.hellochat.backend.dto.UpdateEmailRequest;
import com.hellochat.backend.dto.UpdateFriendRequest;
import com.hellochat.backend.dto.UpdateProfileRequest;
import com.hellochat.backend.dto.UserProfileResponse;
import com.hellochat.backend.entity.EmailCaptcha;
import com.hellochat.backend.entity.FriendRequest;
import com.hellochat.backend.entity.Friendship;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.entity.UserBlock;
import com.hellochat.backend.repository.EmailCaptchaRepository;
import com.hellochat.backend.repository.FriendRequestRepository;
import com.hellochat.backend.repository.FriendshipRepository;
import com.hellochat.backend.repository.UserBlockRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailCaptchaRepository emailCaptchaRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;

    public UserServiceImpl(
        UserRepository userRepository,
        EmailCaptchaRepository emailCaptchaRepository,
        FriendRequestRepository friendRequestRepository,
        FriendshipRepository friendshipRepository,
        UserBlockRepository userBlockRepository
    ) {
        this.userRepository = userRepository;
        this.emailCaptchaRepository = emailCaptchaRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBlockRepository = userBlockRepository;
    }

    @Override
    public UserProfileResponse getMe(Long userId) {
        return getProfile(userId);
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        return new UserProfileResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return new UserProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserProfileResponse updateEmail(Long userId, UpdateEmailRequest request) {
        String email = request.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }
        EmailCaptcha captcha = emailCaptchaRepository
            .findTopByEmailAndSceneOrderByCreatedAtDesc(email, "modify_email")
            .orElseThrow(() -> new IllegalArgumentException("captcha not found"));
        if (captcha.getConsumedAt() != null || !captcha.getCaptcha().equals(request.getCaptcha())
            || captcha.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("captcha invalid");
        }

        User user = findUser(userId);
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        captcha.setConsumedAt(LocalDateTime.now());
        emailCaptchaRepository.save(captcha);
        return new UserProfileResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserProfileResponse> search(String keyword, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        var result = userRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
            safeKeyword,
            safeKeyword,
            PageRequest.of(safePage - 1, safePageSize)
        );
        return new PageResponse<>(
            result.getContent().stream().map(UserProfileResponse::new).toList(),
            safePage,
            safePageSize,
            result.getTotalElements()
        );
    }

    @Override
    public List<FriendResponse> listFriends(Long userId) {
        findUser(userId);
        List<Friendship> friendships = friendshipRepository
            .findByUserAIdAndDeletedAtIsNullOrUserBIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, userId);
        Map<Long, User> users = userRepository.findAllById(friendships.stream().map(friendship -> otherUserId(friendship, userId)).toList())
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
        return friendships.stream()
            .map(friendship -> new FriendResponse(friendship, users.get(otherUserId(friendship, userId))))
            .toList();
    }

    @Override
    public List<FriendRequestResponse> listReceivedFriendRequests(Long userId) {
        findUser(userId);
        return toFriendRequestResponses(friendRequestRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(
            userId,
            FriendRequest.STATUS_PENDING
        ));
    }

    @Override
    public List<FriendRequestResponse> listSentFriendRequests(Long userId) {
        findUser(userId);
        return toFriendRequestResponses(friendRequestRepository.findByRequesterIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(Long userId, CreateFriendRequestRequest request) {
        User requester = findUser(userId);
        User receiver = findUser(request.getReceiverId());
        if (requester.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("cannot add yourself");
        }
        if (isFriend(requester.getId(), receiver.getId())) {
            throw new IllegalArgumentException("already friends");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(requester.getId(), receiver.getId())
            || userBlockRepository.existsByBlockerIdAndBlockedId(receiver.getId(), requester.getId())) {
            throw new IllegalArgumentException("friend request blocked");
        }
        FriendRequest friendRequest = friendRequestRepository.findByRequesterIdAndReceiverId(requester.getId(), receiver.getId())
            .orElseGet(FriendRequest::new);
        if (friendRequest.getId() != null && friendRequest.getStatus() == FriendRequest.STATUS_PENDING) {
            throw new IllegalArgumentException("friend request already pending");
        }
        friendRequest.setRequesterId(requester.getId());
        friendRequest.setReceiverId(receiver.getId());
        friendRequest.setRemark(request.getRemark() == null ? "" : request.getRemark().trim());
        friendRequest.setStatus(FriendRequest.STATUS_PENDING);
        friendRequest.setHandledAt(null);
        friendRequest.setCreatedAt(LocalDateTime.now());
        return new FriendRequestResponse(friendRequestRepository.save(friendRequest), requester, receiver);
    }

    @Override
    @Transactional
    public FriendResponse approveFriendRequest(Long userId, Long requestId) {
        FriendRequest request = requireReceivedRequest(userId, requestId);
        request.setStatus(FriendRequest.STATUS_ACCEPTED);
        request.setHandledAt(LocalDateTime.now());
        friendRequestRepository.save(request);
        Friendship friendship = getOrCreateFriendship(request.getRequesterId(), request.getReceiverId());
        return new FriendResponse(friendship, findUser(request.getRequesterId()));
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        FriendRequest request = requireReceivedRequest(userId, requestId);
        request.setStatus(FriendRequest.STATUS_REJECTED);
        request.setHandledAt(LocalDateTime.now());
        friendRequestRepository.save(request);
    }

    @Override
    @Transactional
    public FriendResponse updateFriend(Long userId, Long friendUserId, UpdateFriendRequest request) {
        Friendship friendship = requireFriendship(userId, friendUserId);
        if (request.getRemarkName() != null) {
            friendship.setRemarkName(request.getRemarkName().trim());
        }
        if (request.getFriendGroup() != null) {
            friendship.setFriendGroup(request.getFriendGroup().trim());
        }
        if (request.getStar() != null) {
            friendship.setStar(Boolean.TRUE.equals(request.getStar()) ? 1 : 0);
        }
        return new FriendResponse(friendshipRepository.save(friendship), findUser(friendUserId));
    }

    @Override
    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {
        Friendship friendship = requireFriendship(userId, friendUserId);
        friendship.setDeletedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);
    }

    @Override
    @Transactional
    public void blockUser(Long userId, Long blockedUserId) {
        findUser(userId);
        findUser(blockedUserId);
        if (userId.equals(blockedUserId)) {
            throw new IllegalArgumentException("cannot block yourself");
        }
        userBlockRepository.findByBlockerIdAndBlockedId(userId, blockedUserId).orElseGet(() -> {
            UserBlock block = new UserBlock();
            block.setBlockerId(userId);
            block.setBlockedId(blockedUserId);
            block.setCreatedAt(LocalDateTime.now());
            return userBlockRepository.save(block);
        });
        friendshipRepository.findByUserAIdAndUserBId(minUserId(userId, blockedUserId), maxUserId(userId, blockedUserId))
            .filter(friendship -> friendship.getDeletedAt() == null)
            .ifPresent(friendship -> {
                friendship.setDeletedAt(LocalDateTime.now());
                friendshipRepository.save(friendship);
            });
    }

    @Override
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        userBlockRepository.findByBlockerIdAndBlockedId(userId, blockedUserId).ifPresent(userBlockRepository::delete);
    }

    @Override
    public List<UserProfileResponse> listBlockedUsers(Long userId) {
        findUser(userId);
        List<Long> blockedIds = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(UserBlock::getBlockedId)
            .toList();
        return userRepository.findAllById(blockedIds).stream().map(UserProfileResponse::new).toList();
    }

    private User findUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private List<FriendRequestResponse> toFriendRequestResponses(List<FriendRequest> requests) {
        List<Long> userIds = requests.stream()
            .flatMap(request -> List.of(request.getRequesterId(), request.getReceiverId()).stream())
            .distinct()
            .toList();
        Map<Long, User> users = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return requests.stream()
            .map(request -> new FriendRequestResponse(request, users.get(request.getRequesterId()), users.get(request.getReceiverId())))
            .toList();
    }

    private FriendRequest requireReceivedRequest(Long userId, Long requestId) {
        findUser(userId);
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("friend request not found"));
        if (!request.getReceiverId().equals(userId) || request.getStatus() != FriendRequest.STATUS_PENDING) {
            throw new IllegalArgumentException("friend request not found");
        }
        return request;
    }

    private Friendship getOrCreateFriendship(Long userId, Long friendUserId) {
        Long userAId = minUserId(userId, friendUserId);
        Long userBId = maxUserId(userId, friendUserId);
        Friendship friendship = friendshipRepository.findByUserAIdAndUserBId(userAId, userBId).orElseGet(() -> {
            Friendship created = new Friendship();
            created.setUserAId(userAId);
            created.setUserBId(userBId);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        friendship.setDeletedAt(null);
        return friendshipRepository.save(friendship);
    }

    private Friendship requireFriendship(Long userId, Long friendUserId) {
        findUser(userId);
        findUser(friendUserId);
        return friendshipRepository.findByUserAIdAndUserBId(minUserId(userId, friendUserId), maxUserId(userId, friendUserId))
            .filter(friendship -> friendship.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("friendship not found"));
    }

    private boolean isFriend(Long userId, Long friendUserId) {
        return friendshipRepository.findByUserAIdAndUserBId(minUserId(userId, friendUserId), maxUserId(userId, friendUserId))
            .filter(friendship -> friendship.getDeletedAt() == null)
            .isPresent();
    }

    private Long otherUserId(Friendship friendship, Long userId) {
        return friendship.getUserAId().equals(userId) ? friendship.getUserBId() : friendship.getUserAId();
    }

    private Long minUserId(Long userId, Long friendUserId) {
        return Math.min(userId, friendUserId);
    }

    private Long maxUserId(Long userId, Long friendUserId) {
        return Math.max(userId, friendUserId);
    }
}

package com.hellochat.backend.repository;

import com.hellochat.backend.entity.FriendRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, Integer status);

    List<FriendRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<FriendRequest> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);
}

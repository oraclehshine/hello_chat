package com.hellochat.backend.repository;

import com.hellochat.backend.entity.GroupJoinRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    List<GroupJoinRequest> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, Integer status);

    List<GroupJoinRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<GroupJoinRequest> findFirstByGroupIdAndRequesterIdAndStatusOrderByCreatedAtDesc(
        Long groupId,
        Long requesterId,
        Integer status
    );
}

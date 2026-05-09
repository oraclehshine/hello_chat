package com.hellochat.backend.repository;

import com.hellochat.backend.entity.GroupMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByUserIdAndLeftAtIsNullAndStatusOrderByJoinedAtDesc(Long userId, Integer status);

    List<GroupMember> findByGroupIdAndLeftAtIsNullAndStatusOrderByRoleDescJoinedAtAsc(Long groupId, Integer status);

    Optional<GroupMember> findByGroupIdAndUserIdAndLeftAtIsNullAndStatus(Long groupId, Long userId, Integer status);

    boolean existsByGroupIdAndUserIdAndLeftAtIsNullAndStatus(Long groupId, Long userId, Integer status);

    long countByGroupIdAndLeftAtIsNullAndStatus(Long groupId, Integer status);

    long countByGroupIdAndLeftAtIsNullAndStatusAndNoticeReadAtGreaterThanEqual(Long groupId, Integer status, java.time.LocalDateTime noticeReadAt);
}

package com.hellochat.backend.repository;

import com.hellochat.backend.entity.Friendship;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserAIdAndDeletedAtIsNullOrUserBIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        Long userAId,
        Long userBId
    );

    Optional<Friendship> findByUserAIdAndUserBId(Long userAId, Long userBId);

    boolean existsByUserAIdAndUserBIdAndDeletedAtIsNull(Long userAId, Long userBId);
}

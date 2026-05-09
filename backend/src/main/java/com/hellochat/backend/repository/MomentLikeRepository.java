package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {

    Optional<MomentLike> findByMomentIdAndUserId(Long momentId, Long userId);

    List<MomentLike> findByMomentIdOrderByCreatedAtAsc(Long momentId);
}

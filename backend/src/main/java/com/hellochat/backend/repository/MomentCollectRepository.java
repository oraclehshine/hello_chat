package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentCollect;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentCollectRepository extends JpaRepository<MomentCollect, Long> {

    Optional<MomentCollect> findByMomentIdAndUserId(Long momentId, Long userId);

    Page<MomentCollect> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

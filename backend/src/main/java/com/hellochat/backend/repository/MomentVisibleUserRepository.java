package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentVisibleUser;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentVisibleUserRepository extends JpaRepository<MomentVisibleUser, Long> {

    boolean existsByMomentIdAndUserId(Long momentId, Long userId);

    List<MomentVisibleUser> findByMomentId(Long momentId);

    List<MomentVisibleUser> findByMomentIdIn(Collection<Long> momentIds);

    void deleteByMomentId(Long momentId);
}

package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentMediaRepository extends JpaRepository<MomentMedia, Long> {

    List<MomentMedia> findByMomentIdInOrderBySortOrderAsc(List<Long> momentIds);

    List<MomentMedia> findByMomentIdOrderBySortOrderAsc(Long momentId);

    void deleteByMomentId(Long momentId);
}

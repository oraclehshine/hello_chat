package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentReportRepository extends JpaRepository<MomentReport, Long> {

    boolean existsByMomentIdAndReporterId(Long momentId, Long reporterId);

    Page<MomentReport> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);

    Page<MomentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

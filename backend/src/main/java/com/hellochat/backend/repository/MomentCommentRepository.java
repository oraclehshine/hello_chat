package com.hellochat.backend.repository;

import com.hellochat.backend.entity.MomentComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentCommentRepository extends JpaRepository<MomentComment, Long> {

    List<MomentComment> findByMomentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long momentId);
}

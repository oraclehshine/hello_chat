package com.hellochat.backend.repository;

import com.hellochat.backend.entity.GroupNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupNotificationRepository extends JpaRepository<GroupNotification, Long> {

    Page<GroupNotification> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
}

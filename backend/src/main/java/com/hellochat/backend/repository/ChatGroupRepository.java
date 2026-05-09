package com.hellochat.backend.repository;

import com.hellochat.backend.entity.ChatGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    Page<ChatGroup> findByNameContainingIgnoreCaseAndStatus(String keyword, Integer status, Pageable pageable);

    boolean existsByInviteCode(String inviteCode);

    java.util.Optional<ChatGroup> findByInviteCodeAndStatus(String inviteCode, Integer status);
}

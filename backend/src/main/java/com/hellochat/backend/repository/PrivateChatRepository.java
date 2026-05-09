package com.hellochat.backend.repository;

import com.hellochat.backend.entity.PrivateChat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateChatRepository extends JpaRepository<PrivateChat, Long> {

    Optional<PrivateChat> findByUserAIdAndUserBId(Long userAId, Long userBId);

    List<PrivateChat> findByUserAIdOrUserBIdOrderByLastMessageAtDescUpdatedAtDesc(Long userAId, Long userBId);
}

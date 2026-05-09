package com.hellochat.backend.repository;

import com.hellochat.backend.entity.GroupMessageMention;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageMentionRepository extends JpaRepository<GroupMessageMention, Long> {

    List<GroupMessageMention> findByMessageIdIn(List<Long> messageIds);
}

package com.hellochat.backend.repository;

import com.hellochat.backend.entity.GroupMessage;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    Page<GroupMessage> findByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(Long groupId, Pageable pageable);

    Page<GroupMessage> findByGroupIdAndContentContainingIgnoreCaseAndDeletedAtIsNullOrderBySentAtDesc(
        Long groupId,
        String keyword,
        Pageable pageable
    );

    Page<GroupMessage> findByGroupIdAndMessageTypeInAndFileIdIsNotNullAndDeletedAtIsNullOrderBySentAtDesc(
        Long groupId,
        java.util.Collection<String> messageTypes,
        Pageable pageable
    );

    long countByGroupIdAndSenderIdNotAndDeletedAtIsNullAndSentAtAfter(Long groupId, Long senderId, LocalDateTime sentAt);

    long countByGroupIdAndSenderIdNotAndDeletedAtIsNull(Long groupId, Long senderId);

    @Query("""
        select count(distinct m.id)
        from GroupMessage m
        left join GroupMessageMention mention on mention.messageId = m.id
        where m.groupId = :groupId
          and m.senderId <> :userId
          and m.deletedAt is null
          and (:lastReadAt is null or m.sentAt > :lastReadAt)
          and (m.mentionAll = 1 or mention.userId = :userId)
        """)
    long countUnreadMentions(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId,
        @Param("lastReadAt") LocalDateTime lastReadAt
    );
}

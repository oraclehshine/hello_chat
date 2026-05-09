package com.hellochat.backend.repository;

import com.hellochat.backend.entity.PrivateMessage;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    Page<PrivateMessage> findByChatIdAndDeletedAtIsNullOrderBySentAtDesc(Long chatId, Pageable pageable);

    Page<PrivateMessage> findByChatIdAndContentContainingIgnoreCaseAndDeletedAtIsNullOrderBySentAtDesc(
        Long chatId,
        String content,
        Pageable pageable
    );

    java.util.Optional<PrivateMessage> findFirstByChatIdAndSenderIdNotAndDeletedAtIsNullOrderBySentAtDesc(
        Long chatId,
        Long senderId
    );

    long countByChatIdAndSenderIdNotAndMessageStatusAndDeletedAtIsNull(Long chatId, Long senderId, Integer messageStatus);

    @Modifying
    @Query("""
        update PrivateMessage message
           set message.messageStatus = :messageStatus,
               message.updatedAt = :updatedAt
         where message.chatId = :chatId
           and message.senderId <> :senderId
           and message.deletedAt is null
           and message.messageStatus <> :messageStatus
        """)
    int markChatMessagesRead(
        @Param("chatId") Long chatId,
        @Param("senderId") Long senderId,
        @Param("messageStatus") Integer messageStatus,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}

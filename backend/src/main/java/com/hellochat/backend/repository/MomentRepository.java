package com.hellochat.backend.repository;

import com.hellochat.backend.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MomentRepository extends JpaRepository<Moment, Long> {

    interface MomentProfileSummaryProjection {
        long getMomentCount();

        long getTotalLikeCount();

        long getTotalCommentCount();

        long getTotalCollectCount();
    }

    Page<Moment> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    Page<Moment> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    @Query("""
        select m from Moment m
        where m.deletedAt is null
          and (
            m.authorId = :userId
            or m.visibility = 'public'
            or (
              m.visibility = 'friends'
              and exists (
                select f.id from Friendship f
                where f.deletedAt is null
                  and f.userAId = case when :userId < m.authorId then :userId else m.authorId end
                  and f.userBId = case when :userId < m.authorId then m.authorId else :userId end
              )
            )
            or (
              m.visibility = 'specified'
              and exists (
                select v.id from MomentVisibleUser v
                where v.momentId = m.id and v.userId = :userId
              )
            )
          )
        order by m.createdAt desc
        """)
    Page<Moment> findVisibleMoments(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select m from Moment m
        where m.authorId = :authorId
          and m.deletedAt is null
          and (
            m.authorId = :userId
            or m.visibility = 'public'
            or (
              m.visibility = 'friends'
              and exists (
                select f.id from Friendship f
                where f.deletedAt is null
                  and f.userAId = case when :userId < m.authorId then :userId else m.authorId end
                  and f.userBId = case when :userId < m.authorId then m.authorId else :userId end
              )
            )
            or (
              m.visibility = 'specified'
              and exists (
                select v.id from MomentVisibleUser v
                where v.momentId = m.id and v.userId = :userId
              )
            )
          )
        order by m.createdAt desc
        """)
    Page<Moment> findVisibleUserMoments(
        @Param("userId") Long userId,
        @Param("authorId") Long authorId,
        Pageable pageable
    );

    @Query("""
        select m from Moment m
        join MomentCollect c on c.momentId = m.id
        where c.userId = :userId
          and m.deletedAt is null
          and (
            m.authorId = :userId
            or m.visibility = 'public'
            or (
              m.visibility = 'friends'
              and exists (
                select f.id from Friendship f
                where f.deletedAt is null
                  and f.userAId = case when :userId < m.authorId then :userId else m.authorId end
                  and f.userBId = case when :userId < m.authorId then m.authorId else :userId end
              )
            )
            or (
              m.visibility = 'specified'
              and exists (
                select v.id from MomentVisibleUser v
                where v.momentId = m.id and v.userId = :userId
              )
            )
          )
        order by c.createdAt desc
        """)
    Page<Moment> findVisibleCollectedMoments(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        select
          count(m) as momentCount,
          coalesce(sum(m.likeCount), 0) as totalLikeCount,
          coalesce(sum(m.commentCount), 0) as totalCommentCount,
          coalesce(sum(m.collectCount), 0) as totalCollectCount
        from Moment m
        where m.authorId = :authorId
          and m.deletedAt is null
        """)
    MomentProfileSummaryProjection summarizeByAuthorId(@Param("authorId") Long authorId);
}

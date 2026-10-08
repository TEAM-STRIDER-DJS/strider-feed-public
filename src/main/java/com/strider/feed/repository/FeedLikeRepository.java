package com.strider.feed.repository;

import com.strider.feed.model.TargetType;
import com.strider.feed.model.entity.FeedLike;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, String> {
    Optional<FeedLike> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, TargetType targetType);

    @Query("""
        select l.targetId as targetId
        from FeedLike l
        where l.userId = :userId
            and l.targetId in :targetIdList
            and l.targetType = :targetType
    """)
    List<String> findByUserIdAndTargetIdInAndTargetType(
            @Param("userId") String userId,
            @Param("targetIdList") List<String> targetIdList,
            @Param("targetType") TargetType targetType
    );

    @Modifying
    @Query(value = """
        insert into feed_like (like_id, user_id, target_type, target_id, created_at)
        values (:likedId, :userId, :targetType, :targetId, now())
        on conflict (user_id, target_type, target_id) do nothing
    """, nativeQuery = true)
    int insertIfAbsent(
        @Param("likedId") String likeId,
        @Param("userId") String userId,
        @Param("targetType") String targetType,
        @Param("targetId") String targetId
    );

    @Modifying
    @Query(value = """
        delete from feed_like
        where user_id = :userId and target_type = :targetType and target_id = :targetId
    """, nativeQuery = true)
    int deleteIfExists(
        @Param("userId") String userId,
        @Param("targetType") String targetType,
        @Param("targetId") String targetId
    );

    @Query("""
        select l.userId
        from FeedLike l
        where
            l.targetId = :feedId
            and l.targetType = com.strider.feed.model.TargetType.FEED
            and (
                l.createdAt < :cursorCreatedAt
                or (l.createdAt = :cursorCreatedAt and l.likeId < :cursorId)
            )
        order by l.createdAt desc, l.likeId desc
    """)
    List<String> findLikeUserIdAfterCursor(
            @Param("feedId") String feedId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    @Query("""
        select l.userId
        from FeedLike l
        where
            l.targetId = :feedId
            and l.targetType = com.strider.feed.model.TargetType.FEED
        order by l.createdAt desc, l.likeId desc
    """)
    List<String> findLikeUserIdFirst(
            @Param("feedId") String feedId,
            Pageable pageable
    );

    @Query("""
        select l.targetId
        from FeedLike l
        where
            l.targetType = com.strider.feed.model.TargetType.COMMENT
            and l.targetId in :commentIds
            and userId = :userId
    """)
    List<String> findLikedCommentIds(@Param("userId") String userId, @Param("commentIds") List<String> commentIds);
}

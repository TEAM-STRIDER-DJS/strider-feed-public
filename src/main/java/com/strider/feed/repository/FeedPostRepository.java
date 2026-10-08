package com.strider.feed.repository;

import com.strider.feed.model.entity.FeedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedPostRepository extends JpaRepository<FeedPost, String> {
    Optional<FeedPost> findByFeedPostIdAndIsDeletedFalse(String feedId);

    // 유저의 (삭제되지 않은) 피드 게시물 수
    long countByUserIdAndIsDeletedFalse(String userId);

    @Query("""
        select p
        from FeedPost p
        where
            p.isDeleted = false
            and p.userId in :followingIds
            and (
                p.createdAt < :cursorCreatedAt
                or (p.createdAt = :cursorCreatedAt and p.feedPostId < :cursorId)
            )
        order by p.createdAt desc, p.feedPostId desc
    """)
    List<FeedPost> findFollowFeedsAfterCursor(
            @Param("followingIds") List<String> followingIds,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    @Query("""
        select p
        from FeedPost p
        where
            p.isDeleted = false
            and p.userId in :followingIds
        order by p.createdAt desc, p.feedPostId desc
    """)
    List<FeedPost> findFollowFeedsFirst(
        @Param("followingIds") List<String> followingIds,
        Pageable pageable
    );

    @Query("""
        select p
        from FeedPost p
        where
            p.isDeleted = false
            and p.userId = :feedUserId
            and (
                p.createdAt < :cursorCreatedAt
                or (p.createdAt = :cursorCreatedAt and p.feedPostId < :cursorId)
            )
        order by p.createdAt desc, p.feedPostId desc
    """)
    List<FeedPost> findUserFeedsAfterCursor(
        @Param("feedUserId") String feedUserId,
        @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
        @Param("cursorId") String cursorId,
        Pageable pageable
    );

    @Query("""
        select p
        from FeedPost p
        where
            p.isDeleted = false
            and p.userId = :feedUserId
        order by p.createdAt desc, p.feedPostId desc
    """)
    List<FeedPost> findUserFeedsFirst(
            @Param("feedUserId") String feedUserId,
            Pageable pageable
    );

    @Modifying
    @Query("""
        update FeedPost p
        set p.likeCount = p.likeCount + 1
        where p.feedPostId = :feedPostId and p.isDeleted = false
    """)
    int incLikeCount(@Param("feedPostId") String feedPostId);

    @Modifying
    @Query("""
        update FeedPost p
        set p.likeCount =
         case when p.likeCount > 0 then p.likeCount-1 else 0 end
        where p.feedPostId = :feedPostId and p.isDeleted = false
    """)
    int decLikeCount(@Param("feedPostId") String feedPostId);

    @Modifying
    @Query("""
        update FeedPost p
        set p.saveCount = p.saveCount + 1
        where p.feedPostId = :feedPostId and p.isDeleted = false
    """)
    int incSaveCount(@Param("feedPostId") String feedPostId);

    @Modifying
    @Query("""
        update FeedPost p
        set p.saveCount =
         case when p.saveCount > 0 then p.saveCount-1 else 0 end
        where p.feedPostId = :feedPostId and p.isDeleted = false
    """)
    int decSaveCount(@Param("feedPostId") String feedPostId);
}

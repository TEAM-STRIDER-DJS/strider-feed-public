package com.strider.feed.repository;

import com.strider.feed.model.entity.FeedComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, String> {
    int countByFeedPostIdAndIsDeletedFalse(String feedPostId);

    @Query("""
         select c.feedPostId as feedPostId,
                count(c) as commentCnt
         from FeedComment c
         where c.isDeleted = false
           and c.feedPostId in :feedPostIds
         group by c.feedPostId
    """)
    List<CommentCountResponse> countByFeedPostIds(List<String> feedPostIds);

    @Query("""
        select c
        from FeedComment c
        where
            c.isDeleted = false
            and c.feedPostId = :feedId
            and c.parentCommentId is null
            and (
                c.createdAt < :cursorCreatedAt
                or (c.createdAt = :cursorCreatedAt and c.commentId < :cursorId)
            )
        order by c.createdAt desc, c.commentId desc
    """)
    List<FeedComment> findCommentsAfterCursor(
            @Param("feedId") String feedId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    @Query("""
        select c
        from FeedComment c
        where
            c.isDeleted = false
            and c.feedPostId = :feedId
            and c.parentCommentId is null
        order by c.createdAt desc, c.commentId desc
    """)
    List<FeedComment> findCommentsFirst(
            @Param("feedId") String feedId,
            Pageable pageable
    );

    @Query("""
        select c
        from FeedComment c
        where
            c.isDeleted = false
            and c.parentCommentId = :parentCommentId
            and (
                c.createdAt < :cursorCreatedAt
                or (c.createdAt = :cursorCreatedAt and c.commentId < :cursorId)
            )
        order by c.createdAt desc, c.commentId desc
    """)
    List<FeedComment> findRepliesAfterCursor(
             @Param("parentCommentId") String parentCommentId,
             @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
             @Param("cursorId") String cursorId,
             Pageable pageable
    );

    @Query("""
        select c
        from FeedComment c
        where
            c.isDeleted = false
            and c.parentCommentId = :parentCommentId
        order by c.createdAt desc, c.commentId desc
    """)
    List<FeedComment> findRepliesFirst(
            @Param("parentCommentId") String parentCommentId,
            Pageable pageable
    );

    @Query("""
        select c.parentCommentId as parentId, count(c) as cnt
        from FeedComment c
        where c.isDeleted = false
          and c.parentCommentId in :parentIds
        group by c.parentCommentId
    """)
    List<ReplyCountRow> countRepliesGroupByParentIds(@Param("parentIds") List<String> parentIds);

    Optional<FeedComment> findByCommentIdAndIsDeletedFalse(String commentId);

    @Modifying
    @Query("""
        update FeedComment c
        set c.likeCount = c.likeCount + 1
        where c.commentId = :commentId and c.isDeleted = false
    """)
    int incLikeCount(@Param("commentId") String commentId);

    @Modifying
    @Query("""
        update FeedComment c
        set c.likeCount =
             case when c.likeCount > 0 then c.likeCount-1 else 0 end
        where c.commentId = :commentId and c.isDeleted = false
    """)
    int decLikeCount(@Param("commentId") String commentId);

    interface ReplyCountRow {
        String getParentId();
        long getCnt();
    }

    interface CommentCountResponse {
        String getFeedPostId();
        int getCommentCnt();
    }
}

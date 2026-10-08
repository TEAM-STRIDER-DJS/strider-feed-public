package com.strider.feed.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedPost {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feed_post_id", nullable = false, unique = true)
    private String feedPostId;

    @Column(name = "exercise_id")
    private String exerciseId;

    @Column(name = "review")
    private String review;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "enable_comment")
    @Builder.Default
    private Boolean enableComment = true;

    @Column(name = "enable_like_cnt")
    @Builder.Default
    private Boolean enableLikeCnt = true;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "save_count", nullable = false)
    @Builder.Default
    private int saveCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

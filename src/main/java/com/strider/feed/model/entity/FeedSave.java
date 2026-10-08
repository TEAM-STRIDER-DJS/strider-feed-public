package com.strider.feed.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feed_save",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_feed_save_user_target",
                columnNames = {"user_id", "target_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedSave {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "save_id", nullable = false, unique = true)
    private String saveId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

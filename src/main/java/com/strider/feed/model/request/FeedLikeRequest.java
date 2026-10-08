package com.strider.feed.model.request;

import com.strider.feed.model.TargetType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public record FeedLikeRequest(
    String userId,
    TargetType targetType,
    String targetId
) {}

package com.strider.feed.model.request;

import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public record FeedSaveRequest(
    String userId,
    String targetId
) {}

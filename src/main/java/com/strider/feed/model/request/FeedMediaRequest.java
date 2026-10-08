package com.strider.feed.model.request;

import com.strider.feed.model.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public record FeedMediaRequest(
    String feedPostId,
    Integer sequence,
    MediaType mediaType,
    String fileUrl,
    String thumbnailUrl
) {}

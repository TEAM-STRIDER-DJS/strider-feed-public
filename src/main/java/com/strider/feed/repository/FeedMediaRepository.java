package com.strider.feed.repository;

import com.strider.feed.model.entity.FeedMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedMediaRepository extends JpaRepository<FeedMedia, String> {
    List<FeedMedia> findByFeedPostIdAndIsDeletedFalseOrderBySequence(String feedPostId);
    Optional<FeedMedia> findFirstByFeedPostIdOrderBySequenceAsc(String feedPostId);
    List<FeedMedia> findByFeedPostIdOrderBySequenceAsc(String feedPostId);
    List<FeedMedia> findAllByIsDeletedFalseAndSequence(int sequence);

    List<FeedMedia> findByFeedPostIdInAndIsDeletedFalseOrderBySequence(List<String> feedPostIdList);
}

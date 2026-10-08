package com.strider.feed.repository;

import com.strider.feed.model.entity.FeedSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedSaveRepository extends JpaRepository<FeedSave, String> {
    Optional<FeedSave> findByUserIdAndTargetId(String userId, String targetId);
    List<FeedSave> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("""
        select s.targetId as targetId
        from FeedSave s
        where s.userId = :userId
            and s.targetId in :targetIdList
    """)
    List<String> findByUserIdAndTargetIdIn(String userId, List<String> targetIdList);

    @Modifying
    @Query(value = """
        insert into feed_save (save_id, user_id, target_id, created_at)
        values (:saveId, :userId, :targetId, now())
        on conflict (user_id, target_id) do nothing
    """, nativeQuery = true)
    int insertIfAbsent(@Param("saveId") String saveId, @Param("userId") String userId, @Param("targetId") String targetId);

    @Modifying
    @Query(value = """
        delete from feed_save
        where user_id = :userId and target_id = :targetId
    """, nativeQuery = true)
    int deleteIfExists(@Param("userId") String userId, @Param("targetId") String targetId);
}

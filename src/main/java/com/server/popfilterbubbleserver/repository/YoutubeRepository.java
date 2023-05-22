package com.server.popfilterbubbleserver.repository;

import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import java.awt.print.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface YoutubeRepository extends JpaRepository<YoutubeChannelEntity, String> {
//    List<String> findTop3CustomIdByTopicIdOrderBySubscriberCountDesc(Integer topicId);
//    List<String> findTopByCustomIdOrderBySubscriberCountDesc(Integer topicId);
    @Query(value = "SELECT channel_id FROM youtube_channel WHERE topic_id = ?1 ORDER BY subscriber_count DESC LIMIT 3", nativeQuery = true)
    List<String> findTop3CustomIdByTopicIdOrderBySubscriberCountDesc(Integer topicId);
}

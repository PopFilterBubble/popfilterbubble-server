package com.server.popfilterbubbleserver.repository;

import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YoutubeRepository extends JpaRepository<YoutubeChannelEntity, String> {
    @Query(value = "SELECT channel_id FROM youtube_channel WHERE topic_id = ?1 ORDER BY subscriber_count DESC LIMIT 3", nativeQuery = true)
    List<String> findTop3IdByTopicIdOrderBySubscriberCountDesc(Integer topicId);

    List<YoutubeChannelEntity> findAllByCustomId(String customId);
}

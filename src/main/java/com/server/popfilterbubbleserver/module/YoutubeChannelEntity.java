package com.server.popfilterbubbleserver.module;


import com.server.popfilterbubbleserver.service.api_response.channel.Items;
import com.server.popfilterbubbleserver.service.api_response.channel.Snippet;
import com.server.popfilterbubbleserver.service.api_response.channel.Statistics;
import com.server.popfilterbubbleserver.service.api_response.channel.TopicDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Setter;

@Entity
@Getter
@Table(name = "youtube_channel")
public class YoutubeChannelEntity {
    @Id
    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "custom_id")
    private String customId;

    @Column(name = "subscriber_count")
    private Integer subscriberCount;

    @Column(name = "video_count")
    private Integer videoCount;

    @Column(name = "topic_id")
    private Integer topicId;

    @Column(name = "politic")
    private Boolean politic;

    public void saveChannelInfo(Snippet snippet, Statistics statistics, String channelId, Boolean politic, Integer topicId) {
        this.channelId = channelId;
        this.title = snippet.getTitle();
        this.description = snippet.getDescription();
        this.customId = snippet.getCustomUrl();
        this.subscriberCount = statistics.getSubscriberCount();
        this.videoCount = statistics.getVideoCount();
        this.topicId = topicId;
        this.politic = politic;
    }
}

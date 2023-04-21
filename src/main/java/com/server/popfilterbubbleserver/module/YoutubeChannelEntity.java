package com.server.popfilterbubbleserver.module;


import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@NoArgsConstructor
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
    private String subscriberCount;

    @Column(name = "video_count")
    private String videoCount;

    @Column(name = "politic")
    private Boolean politic;
}

package com.server.popfilterbubbleserver.service.api_response.video;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Snippet {
    private String publishedAt;
    private String channelId;
    private String title;
    private String description;
    private Thumbnails thumbnails;
    private String channelTitle;
    private String liveBroadcastContent;
    private String publishTime;
}

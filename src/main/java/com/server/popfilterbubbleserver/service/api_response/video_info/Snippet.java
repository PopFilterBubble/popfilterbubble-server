package com.server.popfilterbubbleserver.service.api_response.video_info;

import com.server.popfilterbubbleserver.service.api_response.video.Thumbnails;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Snippet {
    private String publishedAt;
    private String channelId;
    private String title;
    private String description;
    private String channelTitle;
    private String[] tags;
    private String categoryId;
    private Localized localized;
    private Thumbnails thumbnails;
}

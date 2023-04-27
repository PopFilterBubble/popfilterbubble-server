package com.server.popfilterbubbleserver.service.api_response.vidoe_info;

import lombok.Getter;

@Getter
public class Snippet {
    private String publishedAt;
    private String channelId;
    private String title;
    private String description;
    private String channelTitle;
    private String[] tags;
    private String categoryId;
    private Localized localized;
}

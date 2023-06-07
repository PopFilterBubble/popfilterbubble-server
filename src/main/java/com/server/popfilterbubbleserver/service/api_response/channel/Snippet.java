package com.server.popfilterbubbleserver.service.api_response.channel;

import lombok.Getter;

@Getter
public class Snippet {
    private String title;
    private String description;
    private String customUrl;
    private Thumbnails thumbnails;
    private String publishedAt;
}

package com.server.popfilterbubbleserver.service.api_response.channel;

import lombok.Getter;

@Getter
public class Thumbnails {
    private Thumbnail defaultThumbnail;
    private Thumbnail medium;
    private Thumbnail high;
}

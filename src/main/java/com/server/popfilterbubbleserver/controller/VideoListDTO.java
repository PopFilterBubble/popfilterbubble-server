package com.server.popfilterbubbleserver.controller;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoListDTO {

    private String videoId;
    private String title;
    private String description;
    private String channelId;
    private String channelTitle;
    private String channelImg;
    private String thumbnailUrl;
    private String publishedAt;
    private BigInteger viewCount;
    private String url;
}

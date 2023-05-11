package com.server.popfilterbubbleserver.service.api_response.video;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class Thumbnails {


    @JsonProperty("default")
    private Thumbnail defaultThumbnail;
    private Thumbnail medium;
    private Thumbnail high;
}

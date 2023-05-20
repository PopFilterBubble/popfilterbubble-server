package com.server.popfilterbubbleserver.service.api_response.video;

import lombok.Getter;

@Getter
public class VideoApiResult {
    private String nextPageToken;
    private Items items[];
}

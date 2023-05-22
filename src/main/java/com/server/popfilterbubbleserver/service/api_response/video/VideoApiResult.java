package com.server.popfilterbubbleserver.service.api_response.video;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VideoApiResult {
    private String nextPageToken;
    private Items items[];
}

package com.server.popfilterbubbleserver.service.api_response.video_comment;

import lombok.Getter;

@Getter
public class Snippet {
    private String videoId;
    private TopLevelComment topLevelComment;
}

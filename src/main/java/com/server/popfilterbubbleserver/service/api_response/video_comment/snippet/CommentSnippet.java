package com.server.popfilterbubbleserver.service.api_response.video_comment.snippet;

import lombok.Getter;

@Getter
public class CommentSnippet {
    private String videoId;
    private String textDisplay;
    private String textOriginal;
    private String authorDisplayName;
    private Integer likeCount;
    private String publishedAt;
    private String updatedAt;
}

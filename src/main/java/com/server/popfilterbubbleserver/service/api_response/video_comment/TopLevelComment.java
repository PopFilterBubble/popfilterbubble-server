package com.server.popfilterbubbleserver.service.api_response.video_comment;

import com.server.popfilterbubbleserver.service.api_response.video_comment.snippet.CommentSnippet;
import lombok.Getter;


@Getter
public class TopLevelComment {
    private String id;
    private CommentSnippet snippet;
}

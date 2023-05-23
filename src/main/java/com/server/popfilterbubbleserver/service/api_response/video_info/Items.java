package com.server.popfilterbubbleserver.service.api_response.video_info;


import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Items {
    private String id;
    private Snippet snippet;
    private Statistics statistics;
    private TopicDetails topicDetails;
}

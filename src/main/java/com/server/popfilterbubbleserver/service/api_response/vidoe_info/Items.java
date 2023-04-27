package com.server.popfilterbubbleserver.service.api_response.vidoe_info;


import lombok.Getter;

@Getter
public class Items {
    private String id;
    private Snippet snippet;
    private Statistics statistics;
    private TopicDetails topicDetails;
}

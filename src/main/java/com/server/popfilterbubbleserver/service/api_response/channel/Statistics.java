package com.server.popfilterbubbleserver.service.api_response.channel;

import lombok.Getter;

@Getter
public class Statistics {
    private String viewCount;
    private String subscriberCount;
    private String videoCount;
    private Boolean hiddenSubscriberCount;
}
